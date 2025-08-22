package com.petpick.petpick.dto;

import lombok.Data;

@Data
public class HomeCreateRequest {
    private Integer orderId;
    private String receiverName;
    private String receiverPhone;
    private String receiverZip;     // e.g. "100"
    private String receiverAddr;    // 完整收件地址
    private Boolean isCollection;   // true=貨到付款 / 代收
}