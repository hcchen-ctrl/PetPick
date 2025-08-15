package com.petpick.petpick.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.dto.OrderSummaryDTO;
import com.petpick.petpick.service.OrderQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderQueryService orderQueryService;

    // 訂單總覽（使用者全部）
    @GetMapping("/user/{userId}")
    public List<OrderSummaryDTO> list(@PathVariable Integer userId) {
        return orderQueryService.listByUser(userId);
    }

    // 訂單明細（單筆）
    @GetMapping("/{orderId}")
    public OrderDTO getOne(@PathVariable Integer orderId) {
        return orderQueryService.getOne(orderId);
    }
}