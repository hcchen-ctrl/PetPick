package com.petpick.petpick.controller.shop;

import com.petpick.petpick.DTO.shop.EcpayCheckoutRequest;
import com.petpick.petpick.service.shop.EcpayService;
import com.petpick.petpick.service.shop.OrderService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.config.EcpayProperties;


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

}