package com.petpick.petpick.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.Nullable;

import com.petpick.petpick.dto.CartProductDTO;
import com.petpick.petpick.dto.CheckoutRequest;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.dto.UpdateOrderStatusRequest;
import com.petpick.petpick.entity.Order;
import com.petpick.petpick.entity.OrderDetail;
import com.petpick.petpick.entity.OrderPayment;
import com.petpick.petpick.entity.OrderShipment;
import com.petpick.petpick.entity.OrderStatusHistory;
import com.petpick.petpick.entity.Product;
import com.petpick.petpick.repository.OrderDetailRepository;
import com.petpick.petpick.repository.OrderPaymentRepository;
import com.petpick.petpick.repository.OrderRepository;
import com.petpick.petpick.repository.OrderShipmentRepository;
import com.petpick.petpick.repository.OrderStatusHistoryRepository;
import com.petpick.petpick.repository.ProductRepository;
import com.petpick.petpick.repository.ShoppingCartRepository;
import com.petpick.petpick.service.OrderService;
import com.petpick.petpick.service.ShoppingCartService;

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

    // ---------------- Commands ----------------
    @Override
    @Transactional
    public OrderDTO checkout(CheckoutRequest req) {
        Integer userId = req.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        // 1) 撈購物車
        List<CartProductDTO> cartItems = cartService.getCartWithProductByUserId(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalStateException("購物車為空");
        }

        // 2) 計算總額（double → int）
        double total = 0.0d;
        for (CartProductDTO it : cartItems) {
            double unit = it.getPrice();
            int qty = it.getQuantity() == null ? 0 : it.getQuantity();
            total += unit * qty;
        }
        int totalInt = Math.max(0, (int) Math.round(total));

        // 3) 建立訂單（先存訂單，再存出貨）
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalPrice(totalInt);
        order.setStatus("PENDING");
        order.setAddr(req.getAddr());
        order.setReceiverName(req.getReceiverName());
        order.setReceiverPhone(req.getReceiverPhone());
        order.setShippingType(req.getShippingType());
        order.setLogisticsStatus("CREATED");
        order = orderRepo.save(order);

        // 4) 建立明細
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

        // 5) 建立主出貨快照（若為超取，選店回拋再補齊門市資訊）
        OrderShipment ship = new OrderShipment();
        ship.setOrder(order);
        ship.setShippingType(req.getShippingType());      // address / cvs_cod
        ship.setLogisticsSubtype(null);                   // CVS: 之後 set；宅配：例如 TCAT
        ship.setIsCollection("cvs_cod".equalsIgnoreCase(req.getShippingType()));
        ship.setReceiverName(order.getReceiverName());
        ship.setReceiverPhone(order.getReceiverPhone());
        try { // 若有新增 receiverZip 欄位則填
            Order.class.getMethod("getReceiverZip");
            ship.setReceiverZip(order.getReceiverZip());
        } catch (Exception ignore) {}
        ship.setReceiverAddr(order.getAddr());
        ship.setStatus("CREATED");
        shipmentRepo.save(ship);

        // 6) 配送策略
        if ("cvs_cod".equalsIgnoreCase(order.getShippingType())) {
            // CVS 取貨付款：下單即扣庫存 + 清空購物車
            deductStockForOrder(order.getOrderId());
            try { shoppingRepo.deleteByUserId(userId); } catch (Exception ignore) {}
        }
        // 非 CVS：等付款成功才扣庫存與清購物車（見 onPaymentSucceeded/commitReservation）

        // 7) 回傳 DTO
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }

    @Override
    @Transactional
    public void updateStatus(Integer orderId, UpdateOrderStatusRequest req) {
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        String from = nz(o.getStatus());
        String to = nz(req.getStatus());

        // 從 Pending -> Paid：若非 CVS，這裡等同「人工標記收款」，需扣庫存
        if (!"PAID".equalsIgnoreCase(from) && "PAID".equalsIgnoreCase(to)) {
            boolean isCvs = "cvs_cod".equalsIgnoreCase(nz(o.getShippingType()));
            if (!isCvs) {
                deductStockForOrder(orderId);
                commitReservation(orderId); // 清購物車
            }
        }

        // 任意 -> Cancelled：未出貨才回補
        if (!"CANCELLED".equalsIgnoreCase(from) && "CANCELLED".equalsIgnoreCase(to)) {
            boolean shipped = "SHIPPED".equalsIgnoreCase(from) || "IN_TRANSIT".equalsIgnoreCase(nz(o.getLogisticsStatus()));
            if (!shipped) {
                restoreStockForOrder(orderId);
            }
        }

        o.setStatus(to);
        orderRepo.save(o);

        // 紀錄歷程
        statusHistoryRepo.save(newHistory(o, from, to, "admin", nz(req.getNote())));
    }

    @Transactional
    @Override
    public void setStoreInfo(Integer orderId, String brandCodeOrLabel, String storeId, String storeName, String storeAddress) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        o.setShippingType("cvs_cod");                 // 超商取貨付款
        o.setStoreId(storeId);
        o.setStoreName(storeName);
        o.setStoreAddress(storeAddress);
        o.setStoreBrand(normalizeCvsBrand(brandCodeOrLabel));  // 儲存「7-ELEVEN/全家/萊爾富/OK」
        orderRepo.save(o);

        OrderShipment ship = shipmentRepo.findFirstByOrder_OrderIdOrderByCreatedAtAsc(orderId)
            .orElseThrow();
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

    @Override
    @Transactional
    public void setLogisticsInfo(Integer orderId, String logisticsId, String trackingNo) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        o.setLogisticsId(logisticsId);
        o.setTrackingNo(trackingNo);
        orderRepo.save(o);

        shipmentRepo.findFirstByOrder_OrderIdOrderByCreatedAtAsc(orderId)
                .ifPresent(s -> {
                    s.setLogisticsId(logisticsId);
                    s.setTrackingNo(trackingNo);
                    shipmentRepo.save(s);
                });
    }

    // ---------------- Payment callbacks ----------------
// ========== Payment callbacks ==========

@Override
@Transactional
public void onPaymentSucceeded(Integer orderId, String gateway, String tradeNo, int paidAmount) {
    // 介面版本（無 payload），帶 null 進去
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
    pay.setAmount(o.getTotalPrice());
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
    // 介面版本（無 payload），帶 null 進去
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
        if (!shipped) {
            restoreStockForOrder(orderId);
        }

        o.setStatus("CANCELLED");
        // 若有 failReason 欄位就存
        try { Order.class.getMethod("setFailReason", String.class).invoke(o, reason); } catch (Exception ignore) {}
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
        // 付款成功後清空購物車（依據訂單 userId）
        Order o = orderRepo.findById(orderId).orElseThrow();
        Integer userId = o.getUserId();
        if (userId != null) {
            try { cartService.clearCart(userId); } catch (Exception ignore) {}
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

        statusHistoryRepo.save(newHistory(o, "SHIPPED", "admin", "mark shipped"));
    }

    @Transactional
    public void markPickedUp(Integer orderId) { // 超商取件完成
        Order o = orderRepo.findById(orderId).orElseThrow();
        OrderShipment ship = shipmentRepo.findFirstByOrder_OrderIdOrderByCreatedAtAsc(orderId).orElseThrow();

        ship.setStatus("PICKED_UP");
        ship.setReceivedAt(LocalDateTime.now());
        shipmentRepo.save(ship);

        o.setStatus("SHIPPED"); // 或設 DELIVERED：看你的業務定義
        o.setReceivedAt(ship.getReceivedAt());
        o.setLogisticsStatus("PICKED_UP");
        orderRepo.save(o);

        statusHistoryRepo.save(newHistory(o, "SHIPPED", "system", "cvs picked"));
    }

    @Transactional
    public void markDelivered(Integer orderId) { // 宅配成功送達
        Order o = orderRepo.findById(orderId).orElseThrow();
        OrderShipment ship = shipmentRepo.findFirstByOrder_OrderIdOrderByCreatedAtAsc(orderId).orElseThrow();

        ship.setStatus("DELIVERED");
        ship.setDeliveredAt(LocalDateTime.now());
        shipmentRepo.save(ship);

        o.setStatus("SHIPPED"); // 或設 DELIVERED
        o.setDeliveredAt(ship.getDeliveredAt());
        o.setLogisticsStatus("DELIVERED");
        orderRepo.save(o);

        statusHistoryRepo.save(newHistory(o, "SHIPPED", "system", "home delivered"));
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
}