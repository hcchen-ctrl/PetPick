package com.petpick.petpick.DTO.shop;

import lombok.Data;

@Data
public class CartItemRequest {
    private Integer userId;
    private Integer productId;
    private Integer quantity;
}
