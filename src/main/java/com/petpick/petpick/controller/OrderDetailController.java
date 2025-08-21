package com.petpick.petpick.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.petpick.petpick.dto.CreateOrderDetailRequest;
import com.petpick.petpick.dto.OrderDetailDTO;
import com.petpick.petpick.service.OrderDetailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/order-details")
@RequiredArgsConstructor
public class OrderDetailController {

    private final OrderDetailService service;

    // List by orderId
    @GetMapping(params = "orderId")
    public List<OrderDetailDTO> listByOrder(@RequestParam Integer orderId) {
        return service.listByOrderId(orderId);
    }

    // Read one
    @GetMapping("/{detailId}")
    public OrderDetailDTO read(@PathVariable Integer detailId) {
        return service.get(detailId);
    }

    // Create
    @PostMapping
    public OrderDetailDTO create(@RequestBody CreateOrderDetailRequest req) {
        return service.create(req);
    }

    // Update
    @PutMapping("/{detailId}")
    public OrderDetailDTO update(@PathVariable Integer detailId,
                                 @RequestBody CreateOrderDetailRequest req) {
        return service.update(detailId, req);
    }

    // Delete
    @DeleteMapping("/{detailId}")
    public void delete(@PathVariable Integer detailId) {
        service.delete(detailId);
    }
}