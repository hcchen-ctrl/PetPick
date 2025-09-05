// File: src/main/java/com/petpick/petpick/service/shop/impl/OrderServiceImpl.java
package com.petpick.petpick.service.shop.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.petpick.petpick.DTO.shop.CartProductDTO;
import com.petpick.petpick.DTO.shop.CheckoutRequest;
import com.petpick.petpick.DTO.shop.OrderDTO;
import com.petpick.petpick.DTO.shop.UpdateOrderStatusRequest;
import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.entity.shop.Order;
import com.petpick.petpick.entity.shop.OrderDetail;
import com.petpick.petpick.entity.shop.OrderPayment;
import com.petpick.petpick.entity.shop.OrderShipment;
import com.petpick.petpick.entity.shop.OrderStatusHistory;
import com.petpick.petpick.entity.shop.Product;
import com.petpick.petpick.repository.UserRepository;
import com.petpick.petpick.repository.shop.OrderDetailRepository;
import com.petpick.petpick.repository.shop.OrderPaymentRepository;
import com.petpick.petpick.repository.shop.OrderRepository;
import com.petpick.petpick.repository.shop.OrderShipmentRepository;
import com.petpick.petpick.repository.shop.OrderStatusHistoryRepository;
import com.petpick.petpick.repository.shop.ProductRepository;
import com.petpick.petpick.repository.shop.ShoppingCartRepository;
import com.petpick.petpick.service.shop.OrderService;
import com.petpick.petpick.service.shop.ShoppingCartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;
    private final OrderDetailRepository detailRepo;
    private final ProductRepository productRepo;
    private final ShoppingCartService cartService;
    private final ShoppingCartRepository shoppingRepo;
    private final OrderShipmentRepository shipmentRepo;
    private final OrderPaymentRepository paymentRepo;
    private final OrderStatusHistoryRepository statusHistoryRepo;
    private final UserRepository userRepo;

    // ---------------- Commands ----------------
    @Override
    @Transactional
    public OrderDTO checkout(CheckoutRequest req) {
        Integer userId = req.getUserId();
        if (userId == null) throw new IllegalArgumentException("userId is required");

        // 1) 撈購物車
        List<CartProductDTO> cartItems = cartService.getCartWithProductByUserId(userId);
        if (cartItems == null || cartItems.isEmpty()) throw new IllegalStateException("購物車為空");

        // 2) 商品小計（double → int）
        double itemsTotal = 0.0d;
        for (CartProductDTO it : cartItems) {
            double unit = it.getPrice();
            int qty = it.getQuantity() == null ? 0 : it.getQuantity();
            itemsTotal += unit * qty;
        }
        int itemsTotalInt = Math.max(0, (int)Math.round(itemsTotal));

        // 3) 運費（不改資料庫：僅寫進 totalPrice）
        final String shipType = nz(req.getShippingType());
        boolean isHome = "address".equalsIgnoreCase(shipType);
        int shippingFee = 0;
        if (isHome) {
            // 優先讀 CheckoutRequest#getShippingFee()（若 DTO 還沒有此 getter，會自動跳過）
            shippingFee = tryGetIntGetter(req, "getShippingFee", 80);
            if (shippingFee < 0) shippingFee = 80; // 合理下限
        } else {
            shippingFee = 0; // 超取不收運費
        }

        // 4) 折扣目前 0（之後有 coupon 再扣）
        int discount = 0;

        // 5) 含運總額（★寫入現有欄位 totalPrice，不改 schema）
        int grand = Math.max(0, itemsTotalInt + shippingFee - discount);

        // 6) 建立訂單
        Order order = new Order();

        // 從 repo 抓 userEntityObj（你的 Order 已是 @ManyToOne UserEntity）
        UserEntity userEntityObj = userRepo.findById(userId.longValue())
                .orElseThrow(() -> new RuntimeException("User not found"));
        order.setUser(userEntityObj);

        order.setTotalPrice(grand);                 // ★ 關鍵：含運總額
        order.setStatus("PENDING");
        order.setAddr(isHome ? req.getAddr() : "超商取貨付款");
        order.setReceiverName(req.getReceiverName());
        order.setReceiverPhone(req.getReceiverPhone());
        order.setShippingType(shipType);
        order.setLogisticsStatus("CREATED");
        // 若你的 DB 沒自動填 created_at，可在此手動設定
        try {
            Order.class.getMethod("setCreatedAt", LocalDateTime.class).invoke(order, LocalDateTime.now());
        } catch (Exception ignore) {}

        order = orderRepo.save(order);

        // 7) 建立明細
        for (CartProductDTO it : cartItems) {
            Product p = productRepo.findById(it.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + it.getProductId()));
            int unit = (int) Math.round(it.getPrice());

            OrderDetail d = new OrderDetail();
            d.setOrder(order);
            d.setProduct(p);
            d.setQuantity(it.getQuantity());
            d.setUnitPrice(unit);
            detailRepo.save(d);
        }

        // 8) 建立主出貨快照（若為超取，選店回拋再補齊門市資訊）
        OrderShipment ship = new OrderShipment();
        ship.setOrder(order);
        ship.setShippingType(shipType); // address / cvs_cod
        ship.setLogisticsSubtype(null); // CVS: 之後 set；宅配：例如 TCAT
        ship.setIsCollection("cvs_cod".equalsIgnoreCase(shipType));
        ship.setReceiverName(order.getReceiverName());
        ship.setReceiverPhone(order.getReceiverPhone());
        try { // 若有新增 receiverZip 欄位則填
            Order.class.getMethod("getReceiverZip");
            ship.setReceiverZip(order.getReceiverZip());
        } catch (Exception ignore) {}
        ship.setReceiverAddr(order.getAddr());
        ship.setStatus("CREATED");
        shipmentRepo.save(ship);

        // 9) 配送策略
        if ("cvs_cod".equalsIgnoreCase(shipType)) {
            // CVS 取貨付款：下單即扣庫存 + 清空購物車
            deductStockForOrder(order.getOrderId());
            try { shoppingRepo.deleteByUserId(userId); } catch (Exception ignore) {}
        }
        // 非 CVS：等付款成功才扣庫存與清購物車（見 onPaymentSucceeded/commitReservation）

        // 10) 回傳 DTO（totalPrice 已是「含運」）
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        // 若你的 DTO 有對應欄位，可一併回傳拆解資訊（不強制）
        try { OrderDTO.class.getMethod("setItemsTotal", Integer.class).invoke(dto, itemsTotalInt); } catch (Exception ignore) {}
        try { OrderDTO.class.getMethod("setShippingFee", Integer.class).invoke(dto, shippingFee); } catch (Exception ignore) {}
        return dto;
    }

    @Override
    @Transactional
    public void updateStatus(Integer orderId, UpdateOrderStatusRequest req) {
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // ★ 這裡加 trim，避免 "Shipped " 之類值 miss 條件
        final String from = nz(o.getStatus()).trim();
        final String to = nz(req.getStatus()).trim();

        if (!"PAID".equalsIgnoreCase(from) && "PAID".equalsIgnoreCase(to)) {
            boolean isCvs = "cvs_cod".equalsIgnoreCase(nz(o.getShippingType()));
            if (!isCvs) {
                deductStockForOrder(orderId);
                commitReservation(orderId);
            }
        } else if (!"CANCELLED".equalsIgnoreCase(from) && "CANCELLED".equalsIgnoreCase(to)) {
            boolean shipped = "SHIPPED".equalsIgnoreCase(from)
                    || "IN_TRANSIT".equalsIgnoreCase(nz(o.getLogisticsStatus()))
                    || "DELIVERED".equalsIgnoreCase(nz(o.getLogisticsStatus()));
            if (!shipped) restoreStockForOrder(orderId);
            releaseReservation(orderId);
        } else if ("SHIPPED".equalsIgnoreCase(to)) {
            o.setLogisticsStatus("IN_TRANSIT");
            if (o.getShippedAt() == null) { // 避免反覆覆寫
                o.setShippedAt(LocalDateTime.now());
            }
            shipmentRepo.findFirstByOrder_OrderIdOrderByCreatedAtAsc(orderId).ifPresent(s -> {
                s.setStatus("IN_TRANSIT");
                if (s.getShippedAt() == null) s.setShippedAt(o.getShippedAt());
                shipmentRepo.save(s);
            });
        }

        o.setStatus(to);
        orderRepo.save(o);
        statusHistoryRepo.save(newHistory(o, from, to, "admin", nz(req.getNote())));
    }

    @Transactional
    @Override
    public void setStoreInfo(Integer orderId, String brandCodeOrLabel, String storeId, String storeName,
                             String storeAddress) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        o.setShippingType("cvs_cod"); // 超商取貨付款
        o.setStoreId(storeId);
        o.setStoreName(storeName);
        o.setStoreAddress(storeAddress);
        o.setStoreBrand(normalizeCvsBrand(brandCodeOrLabel)); // 儲存「7-ELEVEN/全家/萊爾富/OK」
        orderRepo.save(o);

        OrderShipment ship = shipmentRepo.findFirstByOrder_OrderIdOrderByCreatedAtAsc(orderId).orElseThrow();
        ship.setShippingType("cvs_cod");
        ship.setLogisticsSubtype(brandCodeOrLabel);
        ship.setStoreId(storeId);
        ship.setStoreName(storeName);
        ship.setStoreAddress(storeAddress);
        ship.setStoreBrand(normalizeCvsBrand(brandCodeOrLabel));
        shipmentRepo.save(ship);

        statusHistoryRepo.save(newHistory(o, o.getStatus(), o.getStatus(), "system", "cvs store selected"));
    }

    private String normalizeCvsBrand(String codeOrLabel) {
        if (codeOrLabel == null) return null;
        String s = codeOrLabel.trim().toUpperCase();
        return switch (s) {
            case "UNIMARTC2C", "UNIMART", "7-11", "7ELEVEN", "7-ELEVEN" -> "7-ELEVEN";
            case "FAMIC2C", "FAMI", "FAMILY", "FAMILY MART", "全家" -> "全家";
            case "HILIFEC2C", "HILIFE", "萊爾富" -> "萊爾富";
            case "OKMARTC2C", "OKMART", "OK" -> "OK";
            default -> codeOrLabel; // 未知代碼保留原樣
        };
    }

    @Transactional
    @Override
    public void setLogisticsInfo(Integer orderId, String logisticsId, String trackingNo) {
        orderRepo.findById(orderId).ifPresent(o -> {
            boolean changed = false;

            final String lid = trimOrNull(logisticsId);
            final String tno = trimOrNull(trackingNo);

            if (StringUtils.hasText(lid) && !lid.equals(nz(o.getLogisticsId()))) {
                o.setLogisticsId(lid);
                changed = true;
            }
            if (StringUtils.hasText(tno) && !tno.equals(nz(o.getTrackingNo()))) {
                o.setTrackingNo(tno);
                changed = true;
            }
            if (!StringUtils.hasText(o.getLogisticsStatus())) {
                o.setLogisticsStatus("CREATED");
                changed = true;
            }
            if (!StringUtils.hasText(o.getShippingType())) {
                o.setShippingType("address");
                changed = true;
            }

            // 同步到第一筆出貨紀錄（若存在）
            shipmentRepo.findFirstByOrder_OrderIdOrderByCreatedAtAsc(orderId).ifPresent(s -> {
                boolean sChanged = false;
                if (StringUtils.hasText(lid) && !lid.equals(nz(s.getLogisticsId()))) {
                    s.setLogisticsId(lid);
                    sChanged = true;
                }
                if (StringUtils.hasText(tno) && !tno.equals(nz(s.getTrackingNo()))) {
                    s.setTrackingNo(tno);
                    sChanged = true;
                }
                if (!StringUtils.hasText(nz(s.getStatus()))) {
                    s.setStatus("CREATED");
                    sChanged = true;
                }
                if (sChanged) shipmentRepo.save(s);
            });

            if (changed) {
                orderRepo.saveAndFlush(o); // ★ 立刻 flush，避免以為沒寫入
            }
        });
    }

    // ---------------- Payment callbacks ----------------
    @Override
    @Transactional
    public void onPaymentSucceeded(Integer orderId, String gateway, String tradeNo, int paidAmount) {
        onPaymentSucceededInternal(orderId, gateway, tradeNo, paidAmount, null);
    }

    // 多載：控制器若想帶完整原始 payload 可呼叫這個（非 @Override）
    @Transactional
    public void onPaymentSucceeded(Integer orderId, String gateway, String tradeNo, int paidAmount,
                                   @org.springframework.lang.Nullable String payloadJson) {
        onPaymentSucceededInternal(orderId, gateway, tradeNo, paidAmount, payloadJson);
    }

    private void onPaymentSucceededInternal(Integer orderId, String gateway, String tradeNo, int paidAmount,
                                            @org.springframework.lang.Nullable String payloadJson) {
        Order o = orderRepo.findById(orderId).orElseThrow();

        String prev = nz(o.getStatus());
        if (!"PAID".equalsIgnoreCase(prev)) {
            o.setStatus("PAID");
            o.setPaidAt(LocalDateTime.now());
            o.setPaymentGateway(gateway);
            o.setTradeNo(tradeNo);
            orderRepo.save(o);
            statusHistoryRepo.save(newHistory(o, prev, "PAID", "system", "payment ok"));
        }

        OrderPayment pay = new OrderPayment();
        pay.setOrder(o);
        pay.setGateway(gateway);
        pay.setAmount(paidAmount > 0 ? paidAmount : o.getTotalPrice()); // ★ 以回拋金額為主
        pay.setMerchantTradeNo(o.getMerchantTradeNo());
        pay.setTradeNo(tradeNo);
        pay.setStatus("SUCCESS");
        pay.setPaidAt(LocalDateTime.now());
        pay.setPayloadJson(payloadJson);
        paymentRepo.save(pay);

        boolean isCvs = "cvs_cod".equalsIgnoreCase(nz(o.getShippingType()));
        if (!isCvs) { // 非 CVS：在此才扣庫存與清購物車
            deductStockForOrder(orderId);
            commitReservation(orderId);
        }
    }

    @Override
    @Transactional
    public void onPaymentFailed(Integer orderId, String reason) {
        onPaymentFailedInternal(orderId, reason, null);
    }

    // 多載：控制器若想帶 payload 可呼叫這個（非 @Override）
    @Transactional
    public void onPaymentFailed(Integer orderId, String reason,
                                @org.springframework.lang.Nullable String payloadJson) {
        onPaymentFailedInternal(orderId, reason, payloadJson);
    }

    private void onPaymentFailedInternal(Integer orderId, String reason,
                                         @org.springframework.lang.Nullable String payloadJson) {
        Order o = orderRepo.findById(orderId).orElseThrow();

        String prev = nz(o.getStatus());
        o.setStatus("FAILED");
        o.setPaymentFailReason(reason);
        orderRepo.save(o);
        statusHistoryRepo.save(newHistory(o, prev, "FAILED", "system", reason));

        OrderPayment pay = new OrderPayment();
        pay.setOrder(o);
        pay.setGateway(nz(o.getPaymentGateway()));
        pay.setAmount(o.getTotalPrice());
        pay.setMerchantTradeNo(o.getMerchantTradeNo());
        pay.setTradeNo(o.getTradeNo());
        pay.setStatus("FAIL");
        pay.setFailReason(reason);
        pay.setPayloadJson(payloadJson);
        paymentRepo.save(pay);

        // 若有保留策略可在這裡釋放
        releaseReservation(orderId);
    }

    // ---------------- Cancel ----------------
    @Override
    @Transactional
    public void cancel(Integer orderId) { cancel(orderId, null); }

    @Override
    @Transactional
    public void cancel(Integer orderId, String reason) {
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        String prev = nz(o.getStatus());

        // 未出貨才回補
        boolean shipped = "SHIPPED".equalsIgnoreCase(prev)
                || "IN_TRANSIT".equalsIgnoreCase(nz(o.getLogisticsStatus()))
                || "DELIVERED".equalsIgnoreCase(nz(o.getLogisticsStatus()));
        if (!shipped) restoreStockForOrder(orderId);

        o.setStatus("CANCELLED");
        try {
            Order.class.getMethod("setFailReason", String.class).invoke(o, reason);
        } catch (Exception ignore) {}
        orderRepo.save(o);

        statusHistoryRepo.save(newHistory(o, prev, "CANCELLED", "admin", nz(reason)));
        releaseReservation(orderId);
    }

    // ---------------- Reservation (可日後擴充) ----------------
    @Override
    @Transactional
    public void reserveCart(Integer orderId) {
        // TODO: 若要實作「保留購物車/庫存」，在這裡把 cart 標記 reserved_by_order_id、暫扣庫存
    }

    @Override
    @Transactional
    public void commitReservation(Integer orderId) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        UserEntity userEntityObj = o.getUser(); // 直接拿訂單的 user

        if (userEntityObj != null) {
            try {
                cartService.clearCart(userEntityObj.getUserid().intValue());
            } catch (Exception ignore) {}
        }
        // TODO: 若有庫存保留，這裡做正式扣減
    }

    @Override
    @Transactional
    public void releaseReservation(Integer orderId) {
        // TODO: 若有庫存保留，這裡恢復庫存、解除保留標記
    }

    // ---------------- Stock helpers ----------------
    /** 扣庫存（逐筆明細），原子失敗則拋錯回滾 */
    private void deductStockForOrder(Integer orderId) {
        var details = detailRepo.findByOrder_OrderId(orderId);
        for (OrderDetail d : details) {
            Integer pid = d.getProduct().getProductId();
            int qty = d.getQuantity() == null ? 0 : d.getQuantity();
            if (qty <= 0) continue;
            int updated = productRepo.decreaseStock(pid, qty);
            if (updated != 1) {
                throw new IllegalStateException("商品庫存不足或已變更，無法扣庫存：productId=" + pid);
            }
        }
    }

    /** 回補庫存（逐筆明細） */
    private void restoreStockForOrder(Integer orderId) {
        var details = detailRepo.findByOrder_OrderId(orderId);
        for (OrderDetail d : details) {
            Integer pid = d.getProduct().getProductId();
            int qty = d.getQuantity() == null ? 0 : d.getQuantity();
            if (qty <= 0) continue;
            productRepo.increaseStock(pid, qty);
        }
    }

    // ---------------- Shipping state helpers ----------------
    @Transactional
    public void markShipped(Integer orderId, String logisticsId, String trackingNo) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        OrderShipment ship = shipmentRepo.findFirstByOrder_OrderIdOrderByCreatedAtAsc(orderId).orElseThrow();

        String prev = nz(o.getStatus());

        ship.setStatus("IN_TRANSIT");
        ship.setLogisticsId(logisticsId);
        ship.setTrackingNo(trackingNo);
        ship.setShippedAt(LocalDateTime.now());
        shipmentRepo.save(ship);

        o.setStatus("SHIPPED");
        o.setLogisticsId(logisticsId);
        o.setTrackingNo(trackingNo);
        o.setShippedAt(ship.getShippedAt());
        o.setLogisticsStatus("IN_TRANSIT");
        orderRepo.save(o);

        statusHistoryRepo.save(newHistory(o, prev, "SHIPPED", "admin", "mark shipped"));
    }

    @Transactional
    public void markPickedUp(Integer orderId) { // 超商取件完成
        Order o = orderRepo.findById(orderId).orElseThrow();
        OrderShipment ship = shipmentRepo.findFirstByOrder_OrderIdOrderByCreatedAtAsc(orderId).orElseThrow();

        String prev = nz(o.getStatus());

        ship.setStatus("PICKED_UP");
        ship.setReceivedAt(LocalDateTime.now());
        shipmentRepo.save(ship);

        // 超取可維持 SHIPPED 或改 DELIVERED，依你的業務定義
        o.setStatus("SHIPPED");
        o.setReceivedAt(ship.getReceivedAt());
        o.setLogisticsStatus("PICKED_UP");
        orderRepo.save(o);

        statusHistoryRepo.save(newHistory(o, prev, "SHIPPED", "system", "cvs picked"));
    }

    @Transactional
    public void markDelivered(Integer orderId) { // 宅配成功送達
        Order o = orderRepo.findById(orderId).orElseThrow();
        OrderShipment ship = shipmentRepo.findFirstByOrder_OrderIdOrderByCreatedAtAsc(orderId).orElseThrow();

        String prev = nz(o.getStatus());

        ship.setStatus("DELIVERED");
        ship.setDeliveredAt(LocalDateTime.now());
        shipmentRepo.save(ship);

        o.setStatus("DELIVERED"); // ★ 送達就標 DELIVERED（若你系統只用 SHIPPED，可改回）
        o.setDeliveredAt(ship.getDeliveredAt());
        o.setLogisticsStatus("DELIVERED");
        orderRepo.save(o);

        statusHistoryRepo.save(newHistory(o, prev, "DELIVERED", "system", "home delivered"));
    }

    // ---------------- History helper ----------------
    /** 只給「目標狀態」的版本（from 以當下訂單狀態推斷） */
    private OrderStatusHistory newHistory(Order order, String toStatus, String actor, String note) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(order);
        h.setFromStatus(nz(order.getStatus()));
        h.setToStatus(nz(toStatus));
        h.setActor(nz(actor));
        h.setNote(nz(note));
        return h;
    }

    /** 推薦版本：明確紀錄 from → to */
    private OrderStatusHistory newHistory(Order order, String fromStatus, String toStatus, String actor, String note) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(order);
        h.setFromStatus(nz(fromStatus));
        h.setToStatus(nz(toStatus));
        h.setActor(nz(actor));
        h.setNote(nz(note));
        return h;
    }

    // ---------------- misc utils ----------------
    private static String nz(String s) { return s == null ? "" : s; }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 反射讀取整數 getter（例如 getShippingFee），若不存在或非數字則回傳預設值 */
    private static int tryGetIntGetter(Object obj, String getterName, int defaultValue) {
        try {
            Object v = obj.getClass().getMethod(getterName).invoke(obj);
            if (v instanceof Number n) return (int)Math.round(n.doubleValue());
        } catch (Exception ignore) {}
        return defaultValue;
    }
}