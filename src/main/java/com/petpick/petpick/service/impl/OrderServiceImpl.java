package com.petpick.petpick.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petpick.petpick.dto.CartProductDTO;
import com.petpick.petpick.dto.CheckoutRequest;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.dto.UpdateOrderStatusRequest;
import com.petpick.petpick.entity.Order;
import com.petpick.petpick.entity.OrderDetail;
import com.petpick.petpick.entity.Product;
import com.petpick.petpick.repository.OrderDetailRepository;
import com.petpick.petpick.repository.OrderRepository;
import com.petpick.petpick.repository.ProductRepository;
import com.petpick.petpick.repository.ShoppingCartRepository;
import com.petpick.petpick.service.OrderService;
import com.petpick.petpick.service.ShoppingCartService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;
    private final OrderDetailRepository detailRepo;
    private final ProductRepository productRepo;
    private final ShoppingCartService cartService;
    private final ShoppingCartRepository shoppingRepo;

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

            // 2) 計算總額（double → 轉成整數金額）
            double total = 0.0d;
            for (CartProductDTO it : cartItems) {
                double unit = it.getPrice();                        // ← double
                int qty = it.getQuantity() == null ? 0 : it.getQuantity();
                total += unit * qty;
            }
            int totalInt = Math.max(0, (int) Math.round(total));    // 若需至少 1，可改成 Math.max(1, ...)

            // 3) 建立訂單
            Order order = new Order();
            order.setUserId(userId);
            order.setTotalPrice(totalInt);                          // 若欄位是 BigDecimal，請改 setTotal(...)
            order.setStatus("PENDING");
            order.setAddr(req.getAddr());
            order.setReceiverName(req.getReceiverName());
            order.setReceiverPhone(req.getReceiverPhone());
            order.setShippingType(req.getShippingType());
            order = orderRepo.save(order);

            if ("cvs_cod".equalsIgnoreCase(req.getShippingType())) {
                shoppingRepo.deleteByUserId(userId);
            }

            // 4) 建立明細（單價轉成 BigDecimal）
            for (CartProductDTO it : cartItems) {
                Product p = productRepo.findById(it.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + it.getProductId()));

                int unit = (int) Math.round(it.getPrice()); // 同樣直接四捨五入

                OrderDetail d = new OrderDetail();
                d.setOrder(order);
                d.setProduct(p);
                d.setQuantity(it.getQuantity());
                d.setUnitPrice(unit); // 對應資料表欄位 unit_price（INT）
                detailRepo.save(d);
            }

            // 5) 依你的介面設計：checkout 不清空購物車；等付款成功後在 commitReservation 清空
            // reserveCart(order.getOrderId()); // 若將來要做保留機制，可以在此標記
            // 6) 回傳 DTO
            OrderDTO dto = new OrderDTO();
            dto.setOrderId(order.getOrderId());
            dto.setTotalPrice(order.getTotalPrice());
            dto.setStatus(order.getStatus());
            dto.setCreatedAt(order.getCreatedAt());
            shoppingRepo.deleteByUserId(userId);
            return dto;

        }

        @Override
        @Transactional
        public void updateStatus
        (Integer orderId, UpdateOrderStatusRequest req
            
        ) {
        Order o = orderRepo.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));
            o.setStatus(req.getStatus());
            orderRepo.save(o);
        }

        @Transactional
        @Override
        public void setStoreInfo
        (Integer orderId, String brandCodeOrLabel
        , String storeId, String storeName
        , String storeAddress
            
        ) {
        Order o = orderRepo.findById(orderId).orElseThrow();
            o.setShippingType("cvs_cod");                 // ★ 同步把配送方式設為超商取貨付款
            o.setStoreId(storeId);
            o.setStoreName(storeName);
            o.setStoreAddress(storeAddress);
            o.setStoreBrand(normalizeCvsBrand(brandCodeOrLabel));  // ★ 存中文或標準名稱
            orderRepo.save(o);
        }

    

    private String normalizeCvsBrand(String codeOrLabel) {
        if (codeOrLabel == null) {
            return null;
        }
        String s = codeOrLabel.trim().toUpperCase();
        return switch (s) {
            case "UNIMARTC2C", "UNIMART", "7-11", "7ELEVEN", "7-ELEVEN" ->
                "7-ELEVEN";
            case "FAMIC2C", "FAMI", "FAMILY", "FAMILY MART", "全家" ->
                "全家";
            case "HILIFEC2C", "HILIFE", "萊爾富" ->
                "萊爾富";
            case "OKMARTC2C", "OKMART", "OK" ->
                "OK";
            default ->
                codeOrLabel; // 保留原樣，避免未知代碼遺失
        };
    }

    @Override
    @Transactional
    public void setLogisticsInfo(Integer orderId, String logisticsId, String trackingNo) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        o.setLogisticsId(logisticsId);
        o.setTrackingNo(trackingNo);
        orderRepo.save(o);
    }

    // ---------------- Payment callbacks ----------------
    @Override
    @Transactional
    public void onPaymentSucceeded(Integer orderId, String gateway, String tradeNo, int paidAmount) {
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        String st = o.getStatus();
        if (st != null && "PAID".equalsIgnoreCase(st)) {
            return; // 冪等
        }
        o.setStatus("PAID");
        // 選擇性：若 Order 有這些欄位就寫入
        try {
            Order.class.getMethod("setTradeNo", String.class).invoke(o, tradeNo);
        } catch (Exception ignore) {
        }
        try {
            Order.class.getMethod("setPaidAt", LocalDateTime.class).invoke(o, LocalDateTime.now());
        } catch (Exception ignore) {
        }
        try {
            Order.class.getMethod("setGateway", String.class).invoke(o, gateway);
        } catch (Exception ignore) {
        }
        orderRepo.save(o);

        commitReservation(orderId); // 清空購物車等
    }

    @Override
    @Transactional
    public void onPaymentFailed(Integer orderId, String reason) {
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        o.setStatus("FAILED");
        try {
            Order.class.getMethod("setFailReason", String.class).invoke(o, reason);
        } catch (Exception ignore) {
        }
        orderRepo.save(o);

        releaseReservation(orderId);
    }

    // ---------------- Cancel ----------------
    @Override
    @Transactional
    public void cancel(Integer orderId) {
        cancel(orderId, null);
    }

    @Override
    @Transactional
    public void cancel(Integer orderId, String reason) {
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        o.setStatus("CANCELLED");
        try {
            Order.class.getMethod("setFailReason", String.class).invoke(o, reason);
        } catch (Exception ignore) {
        }
        orderRepo.save(o);

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
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        Integer userId = o.getUserId();
        if (userId != null) {
            try {
                cartService.clearCart(userId);
            } catch (Exception ignore) {
            }
        }
        // TODO: 若有庫存保留，這裡做正式扣減
    }

    @Override
    @Transactional
    public void releaseReservation(Integer orderId) {
        // TODO: 若有庫存保留，這裡恢復庫存、解除保留標記
    }

    private void deductStockForOrder(Integer orderId) {
        var details = detailRepo.findByOrder_OrderId(orderId);
        for (OrderDetail d : details) {
            Integer pid = d.getProduct().getProductId();
            int qty = d.getQuantity() == null ? 0 : d.getQuantity();
            if (qty <= 0) {
                continue;
            }
            int updated = productRepo.decreaseStock(pid, qty);
            if (updated != 1) {
                // 你也可自訂 InsufficientStockException
                throw new IllegalStateException("商品庫存不足或已變更，無法扣庫存：productId=" + pid);
            }
        }
    }

    /**
     * 回補庫存（逐筆明細）
     */
    private void restoreStockForOrder(Integer orderId) {
        var details = detailRepo.findByOrder_OrderId(orderId);
        for (OrderDetail d : details) {
            Integer pid = d.getProduct().getProductId();
            int qty = d.getQuantity() == null ? 0 : d.getQuantity();
            if (qty <= 0) {
                continue;
            }
            productRepo.increaseStock(pid, qty);
        }
    }
}
