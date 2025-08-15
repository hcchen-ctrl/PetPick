package com.petpick.petpick.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OrderSummaryDTO {
    private Integer orderId;
    private LocalDateTime createdAt;
    private Integer totalPrice;
    private String status;
}