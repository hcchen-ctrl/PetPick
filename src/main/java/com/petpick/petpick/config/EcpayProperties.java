package com.petpick.petpick.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 綠界設定：
 * - ecpay.stage：true 走測試站、false 走正式站
 * - ecpay.payment.*：金流金鑰/商店代號 & 付款導回網址
 * - ecpay.logistics.*：物流金鑰/商店代號 & 超商選店回拋網址
 *
 * 注意：Spring Boot relaxed binding 允許 kebab-case ↔ camelCase 對應，
 * 例如 application.properties 可寫 merchant-id 對應到 merchantId。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ecpay")
public class EcpayProperties {

    /** 是否使用測試站（預設 true） */
    private boolean stage = true;

    /** 金流設定（3002607 那組） */
    private Payment payment = new Payment();

    /** 物流設定（2000132 那組） */
    private Logistics logistics = new Logistics();

    /** 付款：伺服器通知（ReturnURL，必須可被外部 HTTPS 存取） */
    private String returnUrl;

    /** 付款：前端導回（OrderResultURL） */
    private String orderResultUrl;

    /** 付款：綠界頁面的「返回商店」按鈕（ClientBackURL） */
    private String clientBackUrl;

    @Data
    public static class Payment {
        /** 金流商店代號（例如測試：3002607） */
        private String merchantId;
        /** 金流 HashKey（測試用：pwFHCqoQZGmho4w6） */
        private String hashKey;
        /** 金流 HashIV（測試用：EkRm7iFT261dpevs） */
        private String hashIv;
    }

    @Data
    public static class Logistics {
        /** 物流商店代號（例如測試：2000132） */
        private String merchantId;
        /** 物流 HashKey（測試用：5294y06JbISpM5x9） */
        private String hashKey;
        /** 物流 HashIV（測試用：v77hoKGq4kWxNNIS） */
        private String hashIv;

        /** 超商選店：ServerReplyURL（地圖選完回你這裡） */
        private String cvsMapReturnUrl;

        // 如需託運單建立/宅配，可再補其它回拋或通知網址
        // private String homeCreateReturnUrl;
        // private String logisticsNotifyUrl;
    }

    /* ===== 便捷端點（避免各處 hardcode）===== */

    /** 付款：Cashier AioCheckOut URL（依 stage 切換） */
    public String getPaymentCashierUrl() {
        return stage
                ? "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5"
                : "https://payment.ecpay.com.tw/Cashier/AioCheckOut/V5";
    }

    /** 物流：超商選店地圖 URL（依 stage 切換） */
    public String getLogisticsMapUrl() {
        return stage
                ? "https://logistics-stage.ecpay.com.tw/Express/map"
                : "https://logistics.ecpay.com.tw/Express/map";
    }

    /** 物流：託運單建立（如果未來要用，可先備好） */
    public String getLogisticsCreateUrl() {
        return stage
                ? "https://logistics-stage.ecpay.com.tw/Express/Create"
                : "https://logistics.ecpay.com.tw/Express/Create";
    }
}
