package com.petpick.petpick.DTO.shop;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutRequest {
    private Integer userId; // 必填：誰要結帳
    private String addr; // 宅配地址 或「到店取貨/超商取貨付款」字樣
    private String receiverName; // 可選：收件人
    private String receiverPhone; // 可選：收件電話
    private String receiverZip;
    private String shippingType; // 可選：cvs_cod / address
    private String paymentMethod; // 可選：credit / cash / cod
}
