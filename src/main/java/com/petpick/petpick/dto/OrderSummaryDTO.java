package com.petpick.petpick.dto;

import java.time.LocalDateTime;


public class OrderSummaryDTO {
    private Integer orderId;
    private Integer totalPrice;
    private String status;
    private String merchantTradeNo;   // ★ 必須存在
    private LocalDateTime createdAt;
    private String shippingType;
    private String addr;
    private String storeId;
    private String storeName;
    private String storeAddress;
    private String storeBrand;
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

    public String getShippingType() {
        return shippingType;
    }

    public void setShippingType(String shippingType) {
        this.shippingType = shippingType;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreAddress() {
        return storeAddress;
    }

    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    public String getStoreBrand() {
        return storeBrand;
    }

    public void setStoreBrand(String storeBrand) {
        this.storeBrand = storeBrand;
    }
}
