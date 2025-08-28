package com.petpick.petpick.DTO.shop;

import lombok.Data;

@Data
public class LogisticsCreateRequest {
    private Integer orderId;
    private String subType;         // UNIMARTC2C / ...
    private String isCollection;    // Y / N
    private String storeId;         // 選店取得
    private String receiverName;    // 收件人
    private String receiverPhone;   // 收件手機
    // 其他可加：商品名稱、備註...
}