package com.petpick.petpick.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class OrderDTO {
    private Integer orderId;
    private String status;
    private Integer totalPrice;
    private LocalDateTime createdAt;

    private String receiverName;     // 你之前在 Order entity 加過
    private String receiverPhone;
    private String shippingType;
    private String addr;
    private String storeName;        // 若有超商門市
    private String storeAddress;

    private List<OrderItemDTO> items;
}
