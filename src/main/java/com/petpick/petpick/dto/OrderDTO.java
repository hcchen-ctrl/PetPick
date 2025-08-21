package com.petpick.petpick.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class OrderDTO {
    private Integer orderId;
    private String  merchantTradeNo;
    private Integer totalPrice;
    private String  status;
    private LocalDateTime createdAt;

    private String shippingType;
    private String addr;
    private String receiverName;     // 你之前在 Order entity 加過
    private String receiverPhone;


    private String storeId;
    private String storeName;
    private String storeAddress;
    private String logisticsId;
    private String trackingNo;

    private String tradeNo;

    private List<OrderDetailDTO> items;
}
