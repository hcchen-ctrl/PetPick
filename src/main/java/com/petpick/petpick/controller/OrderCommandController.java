package com.petpick.petpick.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.petpick.petpick.dto.CheckoutRequest;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.dto.OrderFailRequest;
import com.petpick.petpick.entity.Order;
import com.petpick.petpick.repository.OrderRepository;
import com.petpick.petpick.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderCommandController {

    private final OrderService orderService;
    private final OrderRepository orderRepo;

    @PostMapping(path = "/checkout", consumes = "application/json", produces = "application/json")
    public OrderDTO checkout(@RequestBody CheckoutRequest req, HttpServletRequest http) {
        Integer uid = resolveUserId(req, http);
        if (uid == null) {
            // 比 IllegalArgumentException 更貼近語意
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }
        req.setUserId(uid);                 // ★ 由後端統一決定 userId
        return orderService.checkout(req);
    }

    /** 嘗試從 req、SecurityContext、Session、Header 依序取得 userId */
    private Integer resolveUserId(CheckoutRequest req, HttpServletRequest http) {
        // 1) 舊前端相容
        if (req.getUserId() != null) return req.getUserId();

        // 2) SecurityContext（依你的 UserDetails 實作調整）
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String)) {
                Object principal = auth.getPrincipal();
                if (principal instanceof UserDetails ud) {
                    String name = ud.getUsername();
                    if (name != null && name.matches("\\d+")) return Integer.valueOf(name);
                    // 若你的 UserDetails 有 getId()
                    try {
                        var m = principal.getClass().getMethod("getId");
                        Object id = m.invoke(principal);
                        if (id instanceof Integer i) return i;
                        if (id instanceof String s && s.matches("\\d+")) return Integer.valueOf(s);
                    } catch (Exception ignore) {}
                }
            }
        } catch (Exception ignore) {}

        // 3) Session（若你登入時有 setAttribute("userId", ...)）
        var session = http.getSession(false);
        if (session != null) {
            Object s = session.getAttribute("userId");
            if (s instanceof Integer i) return i;
            if (s instanceof String str && str.matches("\\d+")) return Integer.valueOf(str);
        }

        // 4) 開發用 Header
        String demo = http.getHeader("X-Demo-UserId");
        if (demo != null && demo.matches("\\d+")) return Integer.valueOf(demo);

        return null;
    }

@PatchMapping("/{orderId}/fail")
    public ResponseEntity<?> markFail(@PathVariable Integer orderId,
                                      @RequestBody(required = false) OrderFailRequest req,
                                      @RequestHeader(value = "X-Demo-UserId", required = false) Integer demoUid) {
        Order o = orderRepo.findById(orderId).orElse(null);
        if (o == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("ok", false, "error", "Order not found"));
        }

        // （可選）Demo 使用者檢查
        if (demoUid != null && o.getUserId() != null && !o.getUserId().equals(demoUid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("ok", false, "error", "Forbidden"));
        }

        // 只更新狀態
        o.setStatus("Failed"); // 你的系統若用別的字串（例如 "FAILED" / "付款失敗"）就改這裡
        orderRepo.save(o);

        // 把原因寫進 log，日後要查可從 log 或加一張 OrderLog 表
        log.warn("[OrderFail] orderId={} reason={} detail={}",
                orderId,
                req != null ? req.getReason() : "",
                req != null ? req.getDetail() : "");

        return ResponseEntity.ok(Map.of("ok", true, "status", o.getStatus()));
    }}
