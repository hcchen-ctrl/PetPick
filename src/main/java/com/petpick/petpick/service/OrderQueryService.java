package com.petpick.petpick.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.dto.OrderDetailDTO;
import com.petpick.petpick.dto.OrderSummaryDTO;
import com.petpick.petpick.entity.Order;
import com.petpick.petpick.repository.OrderDetailRepository;
import com.petpick.petpick.repository.OrderRepository;

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
    public List<OrderSummaryDTO> listByUser(Integer userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
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
        dto.setMerchantTradeNo(o.getMerchantTradeNo()); // 別漏 MTN
        return dto;
    }
}