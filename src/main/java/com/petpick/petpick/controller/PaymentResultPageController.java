package com.petpick.petpick.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/payment/v2") // 對應 ecpay.order-result-url
public class PaymentResultPageController {

    /**
     * ECPay 的 OrderResultURL 會用 POST（x-www-form-urlencoded）把結果帶回來。
     * 這裡不做畫面渲染，直接依結果 redirect 到靜態 success.html / fail.html，並把必要參數帶在 query
     * string。
     */
    @PostMapping(value = "/result", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String onOrderResult(@RequestBody MultiValueMap<String, String> form,
            HttpSession session) {
        Map<String, String> p = form.toSingleValueMap();
        String rtnCode = p.getOrDefault("RtnCode", "");
        String orderId = p.getOrDefault("MerchantTradeNo", "");
        String tradeNo = p.getOrDefault("TradeNo", "");
        String rtnMsg = p.getOrDefault("RtnMsg", "交易未完成或被取消");

        session.setAttribute("orderResultParams", p);

        if ("1".equals(rtnCode)) {
            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/success.html")
                    .queryParam("orderId", orderId)
                    .queryParam("TradeNo", tradeNo)
                    .queryParam("ok", "1")
                    .encode()
                    .toUriString();
            return "redirect:" + url;
        } else {
            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/fail.html")
                    .queryParam("orderId", orderId)
                    .queryParam("TradeNo", tradeNo)
                    .queryParam("msg", rtnMsg)
                    .encode()
                    .toUriString();
            return "redirect:" + url;
        }
    }

    @GetMapping("/result")
    public String fallbackGet(@RequestParam Map<String, String> q) {
        String ok = q.get("ok");
        String orderId = q.getOrDefault("orderId", "");
        String tradeNo = q.getOrDefault("TradeNo", "");
        String msg = q.getOrDefault("msg", "交易未完成或頁面已過期");

        String base = "1".equals(ok) ? "/success.html" : "/fail.html";
        var b = ServletUriComponentsBuilder.fromCurrentContextPath().path(base)
                .queryParam("orderId", orderId)
                .queryParam("TradeNo", tradeNo);
        if (!"1".equals(ok)) {
            b.queryParam("msg", msg);
        }
        String url = b.encode().toUriString();
        return "redirect:" + url;
    }
}
