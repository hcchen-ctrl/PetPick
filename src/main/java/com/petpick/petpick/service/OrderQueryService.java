package com.petpick.petpick.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.dto.OrderItemDTO;
import com.petpick.petpick.dto.OrderSummaryDTO;
import com.petpick.petpick.entity.Order;
import com.petpick.petpick.entity.OrderDetail;
import com.petpick.petpick.repository.OrderDetailRepository;
import com.petpick.petpick.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

/**
 * 讀取面（Queries）：給前端的訂單總覽與單筆明細。 回傳一律為 DTO，避免直接曝露 Entity。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepo;
    private final OrderDetailRepository detailRepo;

    public List<OrderSummaryDTO> listByUser(Integer userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    public OrderDTO getOne(Integer orderId) {
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        var details = detailRepo.findByOrder_OrderId(orderId);
        return toDTO(o, details);
    }

    private OrderSummaryDTO toSummary(Order o) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        dto.setOrderId(o.getOrderId());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setTotalPrice(o.getTotalPrice());
        dto.setStatus(o.getStatus());
        return dto;
    }

    private OrderDTO toDTO(Order o, List<OrderDetail> details) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(o.getOrderId());
        dto.setStatus(o.getStatus());
        dto.setTotalPrice(o.getTotalPrice());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setReceiverName(o.getReceiverName());
        dto.setReceiverPhone(o.getReceiverPhone());
        dto.setShippingType(o.getShippingType());
        dto.setAddr(o.getAddr());
        dto.setStoreName(o.getStoreName());
        dto.setStoreAddress(o.getStoreAddress());

        dto.setItems(details.stream().map(d -> {
            OrderItemDTO idto = new OrderItemDTO();
            idto.setProductId(d.getProduct().getProductId());
            // 依你的 Product 欄位名調整
            idto.setPname(d.getProduct().getPname());
            idto.setImageUrl(d.getProduct().getImageUrl());
            idto.setPrice(d.getUnitPrice());
            idto.setQuantity(d.getQuantity());
            return idto;
        }).collect(Collectors.toList()));
        return dto;
    }
}