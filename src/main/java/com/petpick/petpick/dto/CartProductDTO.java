package com.petpick.petpick.dto;

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
    private Integer price;

    // 可以另外寫一個 getter 來回傳小計
    public Integer getSubtotal() {
        return price * quantity;
    }

    
}
