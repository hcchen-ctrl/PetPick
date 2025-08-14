package com.petpick.petpick.dto;

import java.time.LocalDateTime;


public interface CartProductView {
    Integer getCartId();
    Integer getQuantity();
    Integer getUserId();
    LocalDateTime getAddedAt();
    Integer getProductId();
    String getPname();
    String getImageUrl();
    Double getPrice();
    Integer getStock();
}