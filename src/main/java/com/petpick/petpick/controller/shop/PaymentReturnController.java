// src/main/java/com/petpick/petpick/controller/PaymentReturnController.java
package com.petpick.petpick.controller.shop;

import java.util.Map;
import java.util.TreeMap;

import com.petpick.petpick.MAC.EcpayPaymentCheckMac;
import com.petpick.petpick.repository.shop.OrderRepository;
import com.petpick.petpick.service.shop.OrderService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.config.EcpayProperties;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/pay/ecpay")
@RequiredArgsConstructor
@Slf4j
public class PaymentReturnController {

    private final EcpayProperties prop;
    private final OrderRepository orderRepo;
    private final OrderService orderService;

    /**
     * 綠界金流 ReturnURL（Server → Server）
     * - content-type: application/x-www-form-urlencoded
     * - 正式：驗簽成功 && RtnCode=1 才標記 Paid
     * - 測試（prop.isStage()==true）：只要 RtnCode=1 就標記 Paid（放寬驗簽）
     * - 回應必須是「1|OK」且 Content-Type: text/plain
     *
     * 註：不宣告 produces，避免 Accept 協商造成 406。
     */
    @PostMapping(value = "/return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    // 不寫 produces，後面用 ResponseEntity 設定 contentType
    )
    public ResponseEntity<String> handleReturn(@RequestParam Map<String, String> payload) {
        try {
            // 攤平並轉大小寫不敏感 Map
            Map<String, String> ci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            payload.forEach(ci::put);

            final boolean isStage = prop.isStage();
            log.info("[ECPay-Return] env={} recv={}", isStage ? "STAGE" : "PROD", ci);

            String key = prop.getPayment().getHashKey();
            String iv = prop.getPayment().getHashIv();

            // 產生本地 MAC（SHA-256）
            String recvMac = ci.getOrDefault("CheckMacValue", "");
            String localMac = EcpayPaymentCheckMac.generate(ci, key, iv);
            boolean macOK = recvMac.equalsIgnoreCase(localMac);

            String rtnCode = ci.getOrDefault("RtnCode", "0");
            String rtnMsg = ci.getOrDefault("RtnMsg", "");
            String tradeNo = ci.getOrDefault("TradeNo", "");
            String mtn = ci.getOrDefault("MerchantTradeNo", "");
            String orderIdStr = ci.get("CustomField1"); // 送單時放的 orderId

            boolean rtnSuccess = "1".equals(rtnCode);
            // 放寬規則：stage 只看 RtnCode；prod 需 MAC + RtnCode
            boolean pass = isStage ? rtnSuccess : (macOK && rtnSuccess);

            if (!pass) {
                log.warn("[ECPay-Return] NOT PASS (env={}, macOK={}, rtnCode={}, msg={}, MTN={}, TradeNo={})",
                        isStage ? "STAGE" : "PROD", macOK, rtnCode, rtnMsg, mtn, tradeNo);
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("1|OK");
            }

            if (orderIdStr != null && !orderIdStr.isBlank()) {
                try {
                    Integer orderId = Integer.valueOf(orderIdStr.trim());
                    orderRepo.findById(orderId).ifPresent(ord -> {
                        String cur = ord.getStatus() == null ? "" : ord.getStatus();
                        if (!"paid".equalsIgnoreCase(cur)) {
                            ord.setStatus("Paid");
                            // 需要時一併落帳更多資訊（欄位存在再開）
                            // ord.setGateway("ECPAY");
                            // ord.setMerchantTradeNo(mtn);
                            // ord.setTradeNo(tradeNo);
                            // ord.setPaidAt(LocalDateTime.now());
                            orderRepo.save(ord);
                            log.info("[ECPay-Return] order {} -> Paid (env={}, macOK={}, MTN={}, TradeNo={})",
                                    orderId, isStage ? "STAGE" : "PROD", macOK, mtn, tradeNo);
                        } else {
                            log.info("[ECPay-Return] order {} already Paid (idempotent)", orderId);
                        }
                    });
                } catch (NumberFormatException nfe) {
                    log.warn("[ECPay-Return] CustomField1 is not a valid integer: {}", orderIdStr);
                }
            } else {
                log.warn("[ECPay-Return] Missing CustomField1 (orderId). MTN={}, TradeNo={}", mtn, tradeNo);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("1|OK");
        } catch (Exception e) {
            log.error("[ECPay-Return] exception", e);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("1|OK");
        }
    }

    @PostMapping("/callback")
    public String ecpayCallback(@RequestParam Map<String, String> params) {
        // 依你實際欄位取值
        Integer orderId = Integer.valueOf(params.get("CustomField1")); // 例：你把 orderId 放在自訂欄位
        String tradeNo = params.get("TradeNo");
        int amount = Integer.parseInt(params.getOrDefault("TradeAmt", "0"));
        String rtnCode = params.get("RtnCode"); // "1" 表成功

        if ("1".equals(rtnCode)) {
            orderService.onPaymentSucceeded(orderId, "ECPAY", tradeNo, amount);
            return "1|OK";
        } else {
            orderService.onPaymentFailed(orderId, params.getOrDefault("RtnMsg", "FAILED"));
            return "0|FAILED";
        }
    }
}
