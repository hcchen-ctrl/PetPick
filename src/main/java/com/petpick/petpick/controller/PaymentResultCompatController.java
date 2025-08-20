// PaymentResultCompatController.java
package com.petpick.petpick.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/payment")
public class PaymentResultCompatController {

    @PostMapping(value = "/result", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String compatPost() {
        // 內部 forward 到新版 handler，保留 POST form 資料
        return "forward:/payment/v2/result";
    }

    @GetMapping("/result")
    public String compatGet(@RequestParam Map<String, String> q) {
        String url = UriComponentsBuilder.fromPath("/payment/v2/result")
                .queryParam("ok", q.get("ok"))
                .queryParam("orderId", q.get("orderId"))
                .queryParam("TradeNo", q.get("TradeNo"))
                .queryParam("msg", q.get("msg"))
                .build(true).toUriString();
        return "redirect:" + url;
    }
}
