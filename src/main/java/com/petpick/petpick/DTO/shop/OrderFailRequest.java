package com.petpick.petpick.DTO.shop;

import lombok.Data;

@Data
public class OrderFailRequest {
    private String reason; // 例如：ECPay CheckMacValue 驗證錯誤
    private String detail; // 例如：第三方原始訊息、堆疊等
}