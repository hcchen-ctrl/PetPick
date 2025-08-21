package com.petpick.petpick.dto;

import java.time.LocalDateTime;


public class OrderSummaryDTO {
    private Integer orderId;
    private Integer totalPrice;
    private String status;
    private String merchantTradeNo;   // ★ 必須存在
    private LocalDateTime createdAt;
    public Integer getOrderId() {
        return orderId;
    }
    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }
    public Integer getTotalPrice() {
        return totalPrice;
    }
    public void setTotalPrice(Integer totalPrice) {
        this.totalPrice = totalPrice;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getMerchantTradeNo() {
        return merchantTradeNo;
    }
    public void setMerchantTradeNo(String merchantTradeNo) {
        this.merchantTradeNo = merchantTradeNo;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
