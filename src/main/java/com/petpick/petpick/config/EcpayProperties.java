package com.petpick.petpick.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "ecpay")
public class EcpayProperties {

    private boolean stage = true;

    // 金流（信用卡等）
    private Payment payment = new Payment();

    // 物流（超商/宅配）
    private Logistics logistics = new Logistics();

    // 回拋/導回與寄件人資訊
    private String returnUrl;
    private String orderResultUrl;
    private String clientBackUrl;
    private String cvsMapReturnUrl;
    private String logisticsServerReplyUrl;
    private String senderName;
    private String senderPhone;
    private String senderZip;
    private String senderAddress;

    @Data
    public static class Payment {
        private String merchantId;
        private String hashKey;
        private String hashIv;
    }

    @Data
    public static class Logistics {
        private String merchantId;
        private String hashKey;
        private String hashIv;
    }
}
