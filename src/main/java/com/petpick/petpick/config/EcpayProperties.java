package com.petpick.petpick.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "ecpay")
public class EcpayProperties {

    private boolean stage;
    private Payment payment;
    private Logistics logistics;

    private String returnUrl;
    private String orderResultUrl;
    private String clientBackUrl;

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
        private String cvsMapReturnUrl;
    }
}
