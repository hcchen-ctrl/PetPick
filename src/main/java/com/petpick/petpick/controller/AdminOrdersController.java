// File: src/main/java/com/petpick/petpick/controller/AdminOrdersController.java
package com.petpick.petpick.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.admin.AdminOrderSpecs;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.dto.OrderSummaryDTO;
import com.petpick.petpick.dto.UpdateOrderStatusRequest;
import com.petpick.petpick.entity.Order;
import com.petpick.petpick.repository.OrderRepository;
import com.petpick.petpick.service.OrderQueryService;
import com.petpick.petpick.service.OrderService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminOrdersController {

    private final OrderRepository orderRepo;
    private final OrderQueryService queryService;
    private final OrderService orderService;

    // 清單（分頁 + 篩選）
    @GetMapping
    public Page<OrderSummaryDTO> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String delivery,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        page = Math.max(page, 1);
        size = Math.max(size, 1);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Order> spec = Specification.allOf(
                AdminOrderSpecs.keyword(q),
                AdminOrderSpecs.statusEq(status),
                AdminOrderSpecs.delivery(delivery),
                AdminOrderSpecs.dateRange(dateFrom, dateTo)
        );
        Page<Order> p = orderRepo.findAll(spec, pageable);
        return p.map(o -> {
            OrderSummaryDTO dto = new OrderSummaryDTO();
            dto.setOrderId(o.getOrderId());
            dto.setTotalPrice(o.getTotalPrice());
            dto.setStatus(o.getStatus());
            dto.setMerchantTradeNo(o.getMerchantTradeNo());
            dto.setCreatedAt(o.getCreatedAt());
            dto.setShippingType(o.getShippingType());
            dto.setAddr(o.getAddr());
            dto.setStoreId(o.getStoreId());
            dto.setStoreName(o.getStoreName());
            dto.setStoreAddress(o.getStoreAddress());
            dto.setStoreBrand(o.getStoreBrand());
            return dto;
        });
    }

    // 單筆（內含 items）
    @GetMapping("/{orderId}")
    public OrderDTO getOne(@PathVariable Integer orderId) {
        return queryService.getOne(orderId);
    }

    // 單筆更新狀態
    @PatchMapping("/{orderId}/status")
    public void updateStatus(@PathVariable Integer orderId, @RequestBody UpdateOrderStatusRequest req) {
        orderService.updateStatus(orderId, req);
    }

    // 批次狀態
    @RequestMapping(value = "/bulk-status", method = RequestMethod.POST)
    public void bulk(@RequestBody BulkStatusReq req) {
        if (req == null || req.orderIds == null || req.orderIds.isEmpty()) {
            return;
        }
        for (Integer id : req.orderIds) {
            UpdateOrderStatusRequest u = new UpdateOrderStatusRequest();
            u.setStatus(req.status);
            u.setNote(req.note);
            orderService.updateStatus(id, u);
        }
    }

    // 人工標記已付款
    @RequestMapping(value = "/{orderId}/mark-paid", method = RequestMethod.POST)
    public void markPaid(@PathVariable Integer orderId, @RequestBody MarkPaidReq req) {
        String gw = (req.gateway == null || req.gateway.isBlank()) ? "Manual" : req.gateway;
        String tn = req.tradeNo == null ? "" : req.tradeNo;
        int amt = Math.max(0, req.paidAmount == null ? 0 : req.paidAmount);
        orderService.onPaymentSucceeded(orderId, gw, tn, amt);
    }

    // === 設定物流（同時支援 POST / PATCH；兼容 ShipmentNo → trackingNo） ===
    @RequestMapping(
        value = "/{orderId}/logistics",
        method = { RequestMethod.POST, RequestMethod.PATCH }
    )
    public ResponseEntity<Void> setLogistics(@PathVariable Integer orderId,
                                             @RequestBody LogisticsReq req) {
        // 兼容：外部若誤用 ShipmentNo 鍵名，也一併接住；標準鍵名仍為 trackingNo
        final String logisticsId = trimOrNull(req.getLogisticsId());
        final String trackingNo  = firstNonBlank(trimOrNull(req.getTrackingNo()),
                                                 trimOrNull(req.getShipmentNo()));

        // 交給 service（@Transactional 內只在值有變時更新，避免把 NULL 蓋成 ""）
        orderService.setLogisticsInfo(orderId, logisticsId, trackingNo);
        return ResponseEntity.noContent().build(); // 204
    }

    // 取消
    @RequestMapping(value = "/{orderId}/cancel", method = RequestMethod.POST)
    public void cancel(@PathVariable Integer orderId, @RequestBody CancelReq req) {
        orderService.cancel(orderId, req.reason);
    }

    // ====== DTOs ======
    @Data
    public static class BulkStatusReq {
        private List<Integer> orderIds;
        private String status;
        private String note;
    }

    @Data
    public static class MarkPaidReq {
        private String gateway;
        private String tradeNo;
        private Integer paidAmount;
    }

    @Data
    public static class LogisticsReq {
        private String logisticsId;
        private String trackingNo;  // 標準鍵名（前端 / 系統內部使用）
        private String shipmentNo;  // 兼容鍵名（若前端/外部誤用 ShipmentNo 也能吃）
    }

    @Data
    public static class CancelReq {
        private String reason;
    }

    // ===== helpers =====
    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : (b != null && !b.isBlank() ? b : null);
    }
}