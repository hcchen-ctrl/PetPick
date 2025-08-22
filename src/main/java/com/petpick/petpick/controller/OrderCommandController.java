package com.petpick.petpick.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.petpick.petpick.dto.CheckoutRequest;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderCommandController {

    private final OrderService orderService;

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
}