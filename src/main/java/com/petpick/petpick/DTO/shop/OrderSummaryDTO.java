package com.petpick.petpick.DTO.shop;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OrderSummaryDTO {
    private Integer orderId;
    private Integer totalPrice;
    private String status;
    private String merchantTradeNo; // ★ 必須存在
    private LocalDateTime createdAt;
    private String shippingType;
    private String addr;
    private String storeId;
    private String storeName;
    private String storeAddress;
    private String storeBrand;
    private LocalDateTime paidAt;
    private String paymentGateway;
    private String tradeNo;
    private String logisticsStatus;

    // 🔽 新增
    private Long userId;
    private String userName;
}
