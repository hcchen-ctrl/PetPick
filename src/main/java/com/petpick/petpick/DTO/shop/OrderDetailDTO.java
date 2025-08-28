// OrderDetailDTO.java
package com.petpick.petpick.DTO.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class OrderDetailDTO {
    private Integer detailId;
    private Integer orderId;
    private Integer productId;
    private String  pname;
    private Integer unitPrice; // 整數元
    private Integer quantity;
    private Integer subtotal;  // 整數元

    public int getSubtotalSafe() {
        if (subtotal != null) return subtotal;
        int up = unitPrice == null ? 0 : unitPrice;
        int q  = quantity  == null ? 0 : quantity;
        long s = (long) up * (long) q;
        if (s > Integer.MAX_VALUE) s = Integer.MAX_VALUE;
        if (s < 0) s = 0;
        return (int) s;
    }
}