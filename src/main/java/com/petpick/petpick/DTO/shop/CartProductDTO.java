package com.petpick.petpick.DTO.shop;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartProductDTO {

    private Integer cartId;
    private Integer quantity;
    private Integer userId;
    private LocalDateTime addedAt;
    private Integer productId;
    private String pname;
    private String imageUrl;
    private double price;
    private Integer stock;        // ← 這個是「商品庫存」

    // 可以另外寫一個 getter 來回傳小計
    public double getSubtotal() {
        return price * quantity;
    }

    
}
