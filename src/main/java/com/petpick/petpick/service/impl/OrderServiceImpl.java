package com.petpick.petpick.service.impl;

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

        // 2) 計算總額
        long total = 0L;
        for (CartProductDTO it : cartItems) {
            int unit = (int) Math.round(it.getPrice()); // price 是 double，直接四捨五入
            int qty = it.getQuantity() == null ? 0 : it.getQuantity();
            total += (long) unit * qty;
        }

        // 3) 建立訂單
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalPrice((int) total);      // ★ 最後再轉回 int
        order.setStatus("Pending");
        order.setAddr(req.getAddr());
        order.setReceiverName(req.getReceiverName());
        order.setReceiverPhone(req.getReceiverPhone());
        order.setShippingType(req.getShippingType());
        order = orderRepo.save(order);

        // 4) 建立明細
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

        // 5) 清空購物車
        cartService.clearCart(userId);

        // 6) 回傳
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
        o.setStatus(req.getStatus());
        orderRepo.save(o);
    }

    @Override
    @Transactional
    public void setStoreInfo(Integer orderId, String storeId, String storeName, String storeAddress) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        o.setStoreId(storeId);
        o.setStoreName(storeName);
        o.setStoreAddress(storeAddress);
        orderRepo.save(o);
    }

    @Override
    @Transactional
    public void setLogisticsInfo(Integer orderId, String logisticsId, String trackingNo) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        o.setLogisticsId(logisticsId);
        o.setTrackingNo(trackingNo);
        orderRepo.save(o);
    }

    @Override
    @Transactional
    public void cancel(Integer orderId) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        o.setStatus("Cancelled");
        orderRepo.save(o);
    }
}
