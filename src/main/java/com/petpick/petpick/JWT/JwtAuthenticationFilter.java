package com.petpick.petpick.JWT;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    // 定義不需要驗證的路徑（白名單）
    private static final List<String> WHITELIST = List.of(
            "/api/kinds",
            "/api/shelters",
            "/api/ages",
            "/api/sexes",
            "/api/auth/login",
            "/api/auth/register");

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // ✅ 增加除錯資訊
        System.out.println("🔍 JWT Filter: " + method + " " + path);

        // JwtAuthenticationFilter
        if (path.equals("/api/products") && "GET".equalsIgnoreCase(method)) {
            System.out.println("✅ 白名單路徑 (GET /api/products)，直接放行");
            filterChain.doFilter(request, response);
            return;
        }

        // 白名單路徑直接放行
        if (isWhitelisted(path)) {
            System.out.println("✅ 白名單路徑，直接放行: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        // 從 Header 取得 Token
        String token = getTokenFromRequest(request);
        System.out.println("👉 Authorization Header: " + request.getHeader("Authorization"));
        System.out.println("👉 提取的 Token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));

        if (token != null) {
            try {
                String username = jwtUtil.validateTokenAndGetUsername(token);
                System.out.println("🔓 Token 驗證結果, username: " + username);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    System.out.println("👤 載入用戶詳情成功: " + userDetails.getUsername());
                    System.out.println("🔑 用戶權限: " + userDetails.getAuthorities());

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    System.out.println("✅ JWT 驗證成功, username = " + username);
                }
            } catch (Exception e) {
                System.err.println("❌ JWT 驗證失敗: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        } else {
            System.out.println("⚠️ 未提供 JWT Token");
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 判斷是否為白名單路徑
    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }
}