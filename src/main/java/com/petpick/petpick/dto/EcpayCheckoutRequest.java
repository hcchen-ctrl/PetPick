package com.petpick.petpick.dto;

import lombok.Data;

@Data
public class EcpayCheckoutRequest {

    private Integer orderId; // 必填：你剛剛建立好的訂單ID
    private String origin;   // 前端 window.location.origin，用來動態組導回網址
}
