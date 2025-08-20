package com.petpick.petpick.dto;

import lombok.Data;

@Data
public class OrderItemDTO {

    private Integer productId;
    private String pname;      // 顯示用名稱（對應你的前端）
    private Integer price;     // 這裡裝 unitPrice
    private Integer quantity;
    private String imageUrl;   // 可選
}
