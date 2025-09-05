package com.petpick.petpick.controller.shop;

import com.petpick.petpick.DTO.shop.CheckoutRequest;
import com.petpick.petpick.DTO.shop.OrderDTO;
import com.petpick.petpick.DTO.shop.OrderFailRequest;
import com.petpick.petpick.entity.shop.Order;
import com.petpick.petpick.repository.shop.OrderRepository;
import com.petpick.petpick.service.shop.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderCommandController {

    private final OrderService orderService;
    private final OrderRepository orderRepo;

    /**
     * 建立訂單 (Checkout)
     */
    @PostMapping(path = "/checkout", consumes = "application/json", produces = "application/json")
    public OrderDTO checkout(@RequestBody CheckoutRequest req, HttpServletRequest http) {
        Integer uid = resolveUserId(req, http);
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }
        req.setUserId(uid); // ★ 從後端統一決定 userId
        return orderService.checkout(req);
    }

    /**
     * 使用者取消訂單
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> userCancel(
            @PathVariable Integer orderId,
            @RequestBody(required = false) AdminOrdersController.CancelReq req,
            @RequestHeader(value = "X-Demo-UserId", required = false) Integer demoUid) {

        var o = orderRepo.findById(orderId).orElseThrow();

        // 驗證：這筆訂單屬於該使用者
        if (demoUid != null && o.getUser() != null && !demoUid.equals(o.getUser().getUserid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        orderService.cancel(orderId, req != null ? req.getReason() : null);
        return ResponseEntity.noContent().build();
    }

    /**
     * 訂單付款失敗
     */
    @PatchMapping("/{orderId}/fail")
    public ResponseEntity<?> markFail(
            @PathVariable Integer orderId,
            @RequestBody(required = false) OrderFailRequest req,
            @RequestHeader(value = "X-Demo-UserId", required = false) Integer demoUid) {

        Order o = orderRepo.findById(orderId).orElse(null);
        if (o == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("ok", false, "error", "Order not found"));
        }

        if (demoUid != null && o.getUser() != null && !demoUid.equals(o.getUser().getUserid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("ok", false, "error", "Forbidden"));
        }

        o.setStatus("Failed");
        orderRepo.save(o);

        log.warn("[OrderFail] orderId={} reason={} detail={}",
                orderId,
                req != null ? req.getReason() : "",
                req != null ? req.getDetail() : "");

        return ResponseEntity.ok(Map.of("ok", true, "status", o.getStatus()));
    }

    /**
     * 嘗試從 req、SecurityContext、Session、Header 依序取得 userId
     */
    private Integer resolveUserId(CheckoutRequest req, HttpServletRequest http) {
        // 1) 舊前端相容
        if (req.getUserId() != null)
            return req.getUserId();

        // 2) SecurityContext（依你的 UserDetails 實作調整）
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String)) {
                Object principal = auth.getPrincipal();
                if (principal instanceof UserDetails ud) {
                    String name = ud.getUsername();
                    if (name != null && name.matches("\\d+"))
                        return Integer.valueOf(name);
                }
            }
        } catch (Exception ignore) {}

        // 3) Session
        var session = http.getSession(false);
        if (session != null) {
            Object s = session.getAttribute("userId");
            if (s instanceof Integer i)
                return i;
            if (s instanceof String str && str.matches("\\d+"))
                return Integer.valueOf(str);
        }

        // 4) 開發用 Header
        String demo = http.getHeader("X-Demo-UserId");
        if (demo != null && demo.matches("\\d+"))
            return Integer.valueOf(demo);

        return null;
    }
}
