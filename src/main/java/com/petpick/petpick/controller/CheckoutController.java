// src/main/java/com/petpick/petpick/controller/CheckoutController.java
package com.petpick.petpick.controller;

import com.petpick.petpick.dto.CheckoutRequest;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.service.OrderService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 注意：這裡路徑只接 "/checkout"，避免變成 /api/orders/api/orders/checkout
    @PostMapping("/checkout")
    public OrderDTO checkout(@RequestBody CheckoutReq req,
                             @RequestHeader(value = "X-Demo-UserId", required = false) Long userId) {

        CheckoutRequest dto = CheckoutRequest.builder()
            .userId(userId != null ? userId.intValue() : null)
            .addr(req.getAddr())
            .receiverZip(req.getReceiverZip())
            .receiverName(req.getReceiverName())
            .receiverPhone(req.getReceiverPhone())
            .shippingType(req.getShippingType()) // "address" or "cvs_cod"
            .build();

        return orderService.checkout(dto);
    }

    // 提供 Controller 專用的 request model，再由上面映射成正式的 CheckoutRequest
    @Data
    public static class CheckoutReq {
        private String addr;
        private String receiverZip;
        private String receiverName;
        private String receiverPhone;
        private String shippingType; // "address" / "cvs_cod"
    }
}