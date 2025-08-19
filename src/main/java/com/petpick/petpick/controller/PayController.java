package com.petpick.petpick.controller;

import java.util.HashMap;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.EcpayCheckoutRequest;
import com.petpick.petpick.mac.EcpayCheckMac;
import com.petpick.petpick.service.EcpayService;
import com.petpick.petpick.service.OrderService;

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

    /**
     * STEP 1: 前端導向到這裡，回一個會自動 POST 到綠界的表單
     */
    @GetMapping("/checkout")
    public ResponseEntity<String> checkout(@RequestParam Integer orderId) {
        String html = ecpayService.buildAioCheckoutForm(orderId);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping(path = "/checkout", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkoutPost(@RequestBody EcpayCheckoutRequest req) {
        if (req == null || req.getOrderId() == null) {
            return ResponseEntity.badRequest().body("orderId is required");
        }
        String html = (req.getOrigin() == null || req.getOrigin().isBlank())
                ? ecpayService.buildAioCheckoutForm(req.getOrderId())
                : ecpayService.buildAioCheckoutForm(req.getOrderId(), req.getOrigin()); // 有多載就用多載
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    /**
     * STEP 2: 綠界伺服器通知（ReturnURL）— 驗檢查碼、改單狀態、回 1|OK
     */
    @PostMapping(path = "/return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> returnUrl(@RequestBody MultiValueMap<String, String> body) {
        var map = new HashMap<String, String>();
        body.forEach((k, v) -> map.put(k, (v == null || v.isEmpty()) ? "" : v.get(0)));

        // 驗證 CheckMacValue
        boolean ok = EcpayCheckMac.verify(map, prop.getPayment().getHashKey(), prop.getPayment().getHashIv());
        if (!ok) {
            return ResponseEntity.badRequest().body("0|CheckMacValue verify fail");
        }

        // 付款成功 RtnCode=1 才更新
        if ("1".equals(map.get("RtnCode"))) {
            // 取回原來的 orderId（我們放在 CustomField1）
            try {
                Integer orderId = Integer.valueOf(map.get("CustomField1"));
                var req = new com.petpick.petpick.dto.UpdateOrderStatusRequest();
                req.setStatus("Paid");
                orderService.updateStatus(orderId, req);
            } catch (Exception ignore) {
            }
        }

        // 一定要回 "1|OK"，否則綠界會重送（5~15 分後最多四次）。:contentReference[oaicite:5]{index=5}
        return ResponseEntity.ok("1|OK");
    }

}
