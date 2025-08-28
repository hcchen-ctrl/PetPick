// CreateOrderDetailRequest.java
package com.petpick.petpick.DTO.shop;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateOrderDetailRequest {

    @NotNull(message = "orderId is required")
    private Integer orderId;

    @NotNull(message = "productId is required")
    private Integer productId;

    @Min(value = 1, message = "quantity must be >= 1")
    private Integer quantity;

    @Min(value = 1, message = "unitPrice must be >= 1")
    private Integer unitPrice; // ★ 整數單價
}