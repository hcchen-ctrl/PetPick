// File: src/main/java/com/petpick/petpick/service/OrderQueryService.java
package com.petpick.petpick.service.shop;

import java.util.List;

import com.petpick.petpick.DTO.shop.OrderDTO;
import com.petpick.petpick.DTO.shop.OrderDetailDTO;
import com.petpick.petpick.DTO.shop.OrderSummaryDTO;
import com.petpick.petpick.entity.shop.Order;
import com.petpick.petpick.repository.shop.OrderDetailRepository;
import com.petpick.petpick.repository.shop.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import lombok.RequiredArgsConstructor;

/**
 * 讀取面（Queries）：給前端的訂單總覽與單筆明細。回傳一律為 DTO，避免直接曝露 Entity。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepo;
    private final OrderDetailRepository detailRepo;

    /** 訂單總覽（列表） */
    public List<OrderSummaryDTO> listByUser(Long userId) {
        return orderRepo.findByUserUseridOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    /** 單筆訂單（含明細） */
    public OrderDTO getOne(Integer orderId) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        var details = detailRepo.findByOrder_OrderId(orderId);

        OrderDTO dto = new OrderDTO();
        dto.setOrderId(o.getOrderId());
        dto.setMerchantTradeNo(o.getMerchantTradeNo());
        dto.setTradeNo(o.getTradeNo());
        dto.setTotalPrice(o.getTotalPrice());
        dto.setStatus(o.getStatus());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setPaidAt(o.getPaidAt());
        dto.setPaymentGateway(o.getPaymentGateway());

        // ★ 帶出出貨/送達與物流狀態（含備援對應）
        dto.setShippedAt(o.getShippedAt());
        dto.setDeliveredAt(o.getDeliveredAt() != null ? o.getDeliveredAt() : o.getReceivedAt());
        dto.setLogisticsStatus(o.getLogisticsStatus());

        dto.setShippingType(o.getShippingType());
        dto.setAddr(o.getAddr());
        dto.setReceiverName(o.getReceiverName());
        dto.setReceiverPhone(o.getReceiverPhone());

        dto.setStoreId(o.getStoreId());
        dto.setStoreName(o.getStoreName());
        dto.setStoreAddress(o.getStoreAddress());
        dto.setStoreBrand(o.getStoreBrand());
        dto.setLogisticsId(o.getLogisticsId());
        dto.setTrackingNo(o.getTrackingNo());

        dto.setItems(details.stream()
                .map(d -> new OrderDetailDTO(
                        d.getId(),
                        o.getOrderId(),
                        d.getProduct() != null ? d.getProduct().getProductId() : null,
                        d.getProduct() != null ? d.getProduct().getPname() : null,
                        d.getUnitPrice(),
                        d.getQuantity(),
                        d.getSubtotal()))
                .toList());

        return dto;
    }

    // ===== helpers =====
    private OrderSummaryDTO toSummary(Order o) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        dto.setOrderId(o.getOrderId());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setTotalPrice(o.getTotalPrice());
        dto.setStatus(o.getStatus());
        dto.setMerchantTradeNo(o.getMerchantTradeNo());

        // ★ 列表頁也需要的欄位（供前端判斷可否取消、顯示配送摘要）
        dto.setShippingType(o.getShippingType());
        dto.setAddr(o.getAddr());
        dto.setStoreId(o.getStoreId());
        dto.setStoreName(o.getStoreName());
        dto.setStoreAddress(o.getStoreAddress());
        dto.setStoreBrand(o.getStoreBrand());
        dto.setLogisticsStatus(o.getLogisticsStatus());

        return dto;
    }
}