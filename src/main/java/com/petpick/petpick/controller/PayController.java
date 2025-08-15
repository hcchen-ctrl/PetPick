package com.petpick.petpick.controller;

import java.util.HashMap;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.EcpayCheckoutRequest;
import com.petpick.petpick.mac.EcpayCheckMac;
import com.petpick.petpick.service.EcpayService;
import com.petpick.petpick.service.OrderService;
import com.petpick.petpick.dto.UpdateOrderStatusRequest;

@RestController
@RequestMapping("/api/pay/ecpay")
@CrossOrigin(origins = "*")
public class PayController {

    private final EcpayService ecpayService;
    private final EcpayProperties prop;
    private final OrderService orderService;

    public PayController(EcpayService ecpayService, EcpayProperties prop, OrderService orderService) {
        this.ecpayService = ecpayService;
        this.prop = prop;
        this.orderService = orderService;
    }

    // 前端用 POST(JSON) 取回自動送出的 HTML 表單
    @PostMapping(path = "/checkout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> checkoutPost(@RequestBody EcpayCheckoutRequest req) {
        if (req.getOrderId() == null) return ResponseEntity.badRequest().body("orderId is required");
        String html = (req.getOrigin() == null || req.getOrigin().isBlank())
                ? ecpayService.buildAioCheckoutForm(req.getOrderId())
                : ecpayService.buildAioCheckoutForm(req.getOrderId(), req.getOrigin());
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    // 綠界伺服器 ReturnURL 回拋（必須回 1|OK）
    @PostMapping(path = "/return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> returnUrl(@RequestBody MultiValueMap<String, String> body) {
        var map = new HashMap<String, String>();
        body.forEach((k, v) -> map.put(k, (v == null || v.isEmpty()) ? "" : v.get(0)));

        boolean ok = EcpayCheckMac.verify(map, prop.getPayment().getHashKey(), prop.getPayment().getHashIv());
        if (!ok) return ResponseEntity.badRequest().body("0|CheckMacValue verify fail");

        // 付款成功（RtnCode=1）→ 更新訂單狀態
        if ("1".equals(map.get("RtnCode"))) {
            try {
                Integer orderId = Integer.valueOf(map.get("CustomField1")); // 我們把 orderId 放這
                var r = new UpdateOrderStatusRequest();
                r.setStatus("Paid");
                orderService.updateStatus(orderId, r);
            } catch (Exception ignore) {}
        }
        return ResponseEntity.ok("1|OK");
    }
}
