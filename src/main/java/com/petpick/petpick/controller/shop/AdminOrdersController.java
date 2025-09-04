package com.petpick.petpick.controller.shop;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.petpick.petpick.admin.AdminOrderSpecs;
import com.petpick.petpick.DTO.shop.OrderDTO;
import com.petpick.petpick.DTO.shop.OrderSummaryDTO;
import com.petpick.petpick.DTO.shop.UpdateOrderStatusRequest;
import com.petpick.petpick.entity.shop.Order;
import com.petpick.petpick.repository.shop.OrderRepository;
import com.petpick.petpick.service.shop.OrderQueryService;
import com.petpick.petpick.service.shop.OrderService;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrdersController {

    private final OrderRepository orderRepo;
    private final OrderQueryService queryService;
    private final OrderService orderService;

    /** 清單（分頁 + 篩選） */
    @GetMapping
    public Page<OrderSummaryDTO> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String delivery,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), Sort.by("createdAt").descending());

        Specification<Order> spec = Specification
                .where(AdminOrderSpecs.keyword(q))
                .and(AdminOrderSpecs.statusEq(status))
                .and(AdminOrderSpecs.delivery(delivery))
                .and(AdminOrderSpecs.dateRange(dateFrom, dateTo));

        return orderRepo.findAll(spec, pageable).map(o -> {
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
            dto.setPaidAt(o.getPaidAt());
            dto.setPaymentGateway(o.getPaymentGateway());
            dto.setTradeNo(o.getTradeNo());
            // 加上使用者資訊
            if (o.getUser() != null) {
                dto.setUserId(o.getUser().getUserid());
                dto.setUserName(o.getUser().getUsername());
            }
            return dto;
        });
    }

    /** 單筆（內含 items） */
    @GetMapping("/{orderId}")
    public OrderDTO getOne(@PathVariable Integer orderId) {
        return queryService.getOne(orderId);
    }

    /** 更新單筆狀態 */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Integer orderId,
            @RequestBody UpdateOrderStatusRequest req) {
        orderService.updateStatus(orderId, req);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 批次更新狀態 */
    @PostMapping("/bulk-status")
    public ResponseEntity<Map<String, Object>> bulk(@RequestBody BulkStatusReq req) {
        if (req == null || req.orderIds == null || req.orderIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "orderIds is empty"));
        }
        req.orderIds.forEach(id -> {
            UpdateOrderStatusRequest u = new UpdateOrderStatusRequest();
            u.setStatus(req.status);
            u.setNote(req.note);
            orderService.updateStatus(id, u);
        });
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 人工標記已付款 */
    @PostMapping("/{orderId}/mark-paid")
    public ResponseEntity<Map<String, Object>> markPaid(@PathVariable Integer orderId, @RequestBody MarkPaidReq req) {
        String gw = (req == null || isBlank(req.getGateway())) ? "Manual" : req.getGateway().trim();
        String tn = (req == null || req.getTradeNo() == null) ? "" : req.getTradeNo().trim();
        int amt = (req == null || req.getPaidAmount() == null) ? 0 : Math.max(0, req.getPaidAmount());
        orderService.onPaymentSucceeded(orderId, gw, tn, amt);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 設定物流 */
    @RequestMapping(value = "/{orderId}/logistics", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<Void> setLogistics(@PathVariable Integer orderId, @RequestBody LogisticsReq req) {
        String logisticsId = trimToNull(req.getLogisticsId());
        String trackingNo = firstNonBlank(trimToNull(req.getTrackingNo()), trimToNull(req.getShipmentNo()));
        orderService.setLogisticsInfo(orderId, logisticsId, trackingNo);
        return ResponseEntity.noContent().build();
    }

    /** 取消 */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable Integer orderId, @RequestBody CancelReq req) {
        orderService.cancel(orderId, req == null ? null : req.getReason());
        return ResponseEntity.ok(Map.of("ok", true));
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
        private String trackingNo;
        private String shipmentNo;
    }

    @Data
    public static class CancelReq {
        private String reason;
    }

    // ===== helpers =====
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : ((b != null && !b.isBlank()) ? b : null);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
