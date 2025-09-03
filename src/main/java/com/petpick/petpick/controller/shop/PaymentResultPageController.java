// PaymentResultPageController.java
package com.petpick.petpick.controller.shop;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/payment/v2") // 對應 ecpay.order-result-url（指到後端）
public class PaymentResultPageController {

    // 前端 SPA 的網域（例：http://localhost:5173 或 https://<你的前端 ngrok>）
    @Value("${app.frontend-base-url}")
    private String frontendBase;

    @PostMapping("/result")
    public String onOrderResult(@RequestBody MultiValueMap<String, String> form, HttpSession session) {
        Map<String, String> p = form.toSingleValueMap();

        boolean ok         = "1".equals(p.getOrDefault("RtnCode", ""));
        String mtn         = p.getOrDefault("MerchantTradeNo", "");
        String tradeNo     = p.getOrDefault("TradeNo", "");
        String orderId     = p.getOrDefault("CustomField1", "");
        String rtnMsg      = p.getOrDefault("RtnMsg", "交易未完成或被取消");
        String paymentType = p.getOrDefault("PaymentType", "");
        String paymentDate = p.getOrDefault("PaymentDate", ""); // 可能含空白
        String tradeAmt    = p.getOrDefault("TradeAmt", "");

        // 可視需要保留
        session.setAttribute("orderResultParams", p);

        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl(frontendBase)
                .path(ok ? "/success" : "/fail")
                .queryParam("mtn", mtn)
                .queryParam("tradeNo", tradeNo)
                .queryParam("orderId", orderId);

        if (ok) {
            b.queryParam("PaymentType", paymentType)
             .queryParam("PaymentDate", paymentDate) // 交由 encode() 處理空白與斜線
             .queryParam("TradeAmt", tradeAmt)
             .queryParam("ok", "1");
        } else {
            b.queryParam("msg", rtnMsg);
        }

        String url = b.build().encode().toUriString(); // ★ 避免 "Invalid character ' '" 例外
        return "redirect:" + url;
    }

    @GetMapping("/result")
    public String fallbackGet(@RequestParam Map<String, String> q) {
        boolean ok = "1".equals(q.get("ok"));

        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl(frontendBase)
                .path(ok ? "/success" : "/fail")
                .queryParam("orderId", q.getOrDefault("orderId", ""))
                .queryParam("TradeNo", q.getOrDefault("TradeNo", ""));

        if (!ok) {
            b.queryParam("msg", q.getOrDefault("msg", "交易未完成或頁面已過期"));
        }

        String url = b.build().encode().toUriString();
        return "redirect:" + url;
    }
}