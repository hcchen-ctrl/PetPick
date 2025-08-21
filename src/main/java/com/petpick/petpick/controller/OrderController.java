package com.petpick.petpick.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.dto.CheckoutRequest;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.dto.OrderDetailDTO;
import com.petpick.petpick.dto.OrderSummaryDTO;
import com.petpick.petpick.repository.OrderDetailRepository;
import com.petpick.petpick.repository.OrderRepository;
import com.petpick.petpick.service.OrderQueryService;
import com.petpick.petpick.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderQueryService orderQueryService;
    private final OrderService orderService;

    // ★ 新增的相依：用來抓明細 & 以綠界編號查單
    private final OrderDetailRepository orderDetailRepository;
    private final OrderRepository orderRepository;

    /** 訂單總覽（某使用者） */
    @GetMapping("/user/{userId}")
    public List<OrderSummaryDTO> list(@PathVariable Integer userId) {
        return orderQueryService.listByUser(userId);
    }

    /** 訂單抬頭＋（若服務有帶）明細 */
    @GetMapping("/{orderId}")
    public OrderDTO getOne(@PathVariable Integer orderId) {
        return orderQueryService.getOne(orderId);
    }

    /** ★ 新增：只取明細（給前端後備用） */
    @GetMapping("/{orderId}/details")
    public List<OrderDetailDTO> details(@PathVariable Integer orderId) {
        return orderDetailRepository.findDTOsByOrderId(orderId);
    }

    /** ★ 新增：用綠界交易序號（TradeNo）查單 */
    @GetMapping("/by-tradeno/{tradeNo}")
    public OrderDTO getByTradeNo(@PathVariable String tradeNo) {
        var order = orderRepository.findByTradeNo(tradeNo)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found by TradeNo"));
        return orderQueryService.getOne(order.getOrderId());
    }

    /** ★ 新增：用綠界訂單編號（MerchantTradeNo）查單 */
    @GetMapping("/by-mtn/{mtn}")
    public OrderDTO getByMtn(@PathVariable String mtn) {
        var order = orderRepository.findByMerchantTradeNo(mtn)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found by MerchantTradeNo"));
        return orderQueryService.getOne(order.getOrderId());
    }

    /** 建立訂單（結帳） */
    @PostMapping(path = "/checkout", consumes = "application/json", produces = "application/json")
    public OrderDTO checkout(@RequestBody CheckoutRequest req) {
        return orderService.checkout(req);
    }
}