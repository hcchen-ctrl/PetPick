package com.petpick.petpick.controller;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class PaymentResultController {

    @PostMapping(value = "/payment/result", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> paymentResult(@RequestBody MultiValueMap<String,String> form) {
        // 不在這裡更新狀態；真正更新請在 ReturnURL
        String orderId = form.getFirst("CustomField1"); // 你在送單時有塞的話
        String location = (orderId != null) ? "/order.html?orderId=" + orderId : "/order.html";
        return ResponseEntity.status(302).header("Location", location).build();
    }

    @GetMapping("/payment/result")
    public ResponseEntity<Void> paymentResultGet() {
        return ResponseEntity.status(302).header("Location", "/order.html").build();
    }
}