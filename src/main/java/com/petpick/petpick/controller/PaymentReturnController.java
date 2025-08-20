package com.petpick.petpick.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.mac.EcpayCheckMac;
import com.petpick.petpick.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/pay/ecpay")
@RequiredArgsConstructor
@Slf4j
public class PaymentReturnController {

    private final EcpayProperties prop;
    private final OrderRepository orderRepo;

    @PostMapping("/return")
    public ResponseEntity<String> handleReturn(@RequestParam Map<String, String> p) {
        try {
            log.info("[ECPay-Return] payload={}", p);

            String key = prop.getPayment().getHashKey();
            String iv  = prop.getPayment().getHashIv();
            String recvMac  = p.getOrDefault("CheckMacValue", "");
            String localMac = EcpayCheckMac.generate(p, key, iv);
            boolean macOK = recvMac.equalsIgnoreCase(localMac);

            String rtnCode = p.getOrDefault("RtnCode", "0");
            String orderIdStr = p.getOrDefault("CustomField1", null);
            String tradeNo = p.getOrDefault("TradeNo", "");

            if (!macOK) {
                log.warn("[ECPay-Return] CheckMacValue mismatch! recv={}, local={}", recvMac, localMac);
            }

            // 測試/容錯：若 RtnCode=1 就寫 Paid（正式可改為 macOK && RtnCode=1 才寫入）
            if ("1".equals(rtnCode) && orderIdStr != null) {
                Integer orderId = Integer.valueOf(orderIdStr);
                orderRepo.findById(orderId).ifPresent(ord -> {
                    if (!"Paid".equalsIgnoreCase(ord.getStatus())) {
                        ord.setStatus("Paid");
                        // ord.setTransactionNo(tradeNo);
                        orderRepo.save(ord);
                        log.info("[ECPay-Return] order {} marked Paid, TradeNo={}", orderId, tradeNo);
                    }
                });
            }

            return ResponseEntity.ok("1|OK");
        } catch (Exception e) {
            log.error("[ECPay-Return] error", e);
            return ResponseEntity.ok("1|OK"); // 仍回 1|OK，避免重送
        }
    }
}