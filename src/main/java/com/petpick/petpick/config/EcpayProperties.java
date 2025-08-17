package com.petpick.petpick.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "ecpay")
public class EcpayProperties {

    /**
     * 測試環境(true) / 正式環境(false)
     */
    private boolean stage = true;

    /**
     * 付款結果伺服器回拋（必須對外可達且 HTTPS）
     */
    private String returnUrl;

    /**
     * 用戶端導回頁（付款完成導回）
     */
    private String orderResultUrl;

    /**
     * 綠界頁面的「返回商店」按鈕
     */
    private String clientBackUrl;

    /**
     * （可選）超商選店回拋
     */
    private String cvsMapReturnUrl;

    /**
     * （可選）物流狀態回拋
     */
    private String logisticsServerReplyUrl;

    /**
     * （可選）物流寄件人資訊
     */
    private String senderName;
    private String senderPhone;
    private String senderZip;
    private String senderAddress;

    /**
     * 付款用金鑰組
     */
    @NestedConfigurationProperty
    private Payment payment = new Payment();

    /**
     * （可選）物流用金鑰組
     */
    @NestedConfigurationProperty
    private Logistics logistics = new Logistics();

    // ---- 若你舊程式曾直接用 prop.getMerchantId() 等，可保留以下欄位做相容（非必要）----
    private String merchantId; // optional: 兼容舊版
    private String hashKey;    // optional: 兼容舊版
    private String hashIv;     // optional: 兼容舊版

    @Data
    public static class Payment {

        /**
         * 例如：3002607（測試）
         */
        private String merchantId;
        /**
         * 例如：pwFHCqoQZGmho4w6（測試）
         */
        private String hashKey;
        /**
         * 例如：EkRm7iFT261dpevs（測試）
         */
        private String hashIv;
    }

    @Data
    public static class Logistics {

        /**
         * 例如：2000132（測試）
         */
        private String merchantId;
        /**
         * 例如：5294y06JbISpM5x9（測試）
         */
        private String hashKey;
        /**
         * 例如：v77hoKGq4kWxNNIS（測試）
         */
        private String hashIv;
    }
}
