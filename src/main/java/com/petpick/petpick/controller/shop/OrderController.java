package com.petpick.petpick.controller.shop;

import java.util.List;

import com.petpick.petpick.DTO.shop.OrderDTO;
import com.petpick.petpick.DTO.shop.OrderDetailDTO;
import com.petpick.petpick.DTO.shop.OrderSummaryDTO;
import com.petpick.petpick.repository.shop.OrderDetailRepository;
import com.petpick.petpick.repository.shop.OrderRepository;
import com.petpick.petpick.service.shop.OrderQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;




import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderQueryService orderQueryService;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderRepository orderRepository;

    /** 訂單總覽（某使用者） */
    @GetMapping("/user/{userId}")
    public List<OrderSummaryDTO> list(@PathVariable Integer userId) {
        return orderQueryService.listByUser(userId);
    }

    /** 訂單抬頭＋可能含明細 */
    @GetMapping("/{orderId}")
    public OrderDTO getOne(@PathVariable Integer orderId) {
        return orderQueryService.getOne(orderId);
    }

    /** 只取明細（前端後備用） */
    @GetMapping("/{orderId}/details")
    public List<OrderDetailDTO> details(@PathVariable Integer orderId) {
        return orderDetailRepository.findDTOsByOrderId(orderId);
    }

    /** 以綠界交易序號查單 */
    @GetMapping("/by-tradeno/{tradeNo}")
    public OrderDTO getByTradeNo(@PathVariable String tradeNo) {
        var order = orderRepository.findByTradeNo(tradeNo)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                HttpStatus.NOT_FOUND, "Order not found by TradeNo"));
        return orderQueryService.getOne(order.getOrderId());
    }

    /** 以綠界訂單編號查單 */
    @GetMapping("/by-mtn/{mtn}")
    public OrderDTO getByMtn(@PathVariable String mtn) {
        var order = orderRepository.findByMerchantTradeNo(mtn)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                HttpStatus.NOT_FOUND, "Order not found by MerchantTradeNo"));
        return orderQueryService.getOne(order.getOrderId());
    }

}