package com.petpick.petpick.JWT;

import java.io.IOException;
import java.util.List;

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

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    // 不需要驗證的 API 前綴（相等或前綴 + "/" 視為白名單）
    private static final List<String> WHITELIST = List.of(
            // 認證
            "/api/auth/login",
            "/api/auth/register",

            // 公開查詢
            "/api/kinds",
            "/api/shelters",
            "/api/ages",
            "/api/sexes",
            "/api/products",

            // 金流/物流（S2S 或不需登入）
            "/api/pay/ecpay/return",
            "/payment",
            "/favicon.ico",
            "/api/logistics/home/reply",
            "/api/logistics/cvs/store-return",
            "/api/logistics/cvs/ecpay/create-return");

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final String uri = request.getRequestURI();
        final String method = request.getMethod();
        // 只處理 /api/**；非 API 或 CORS 預檢一律略過
        return !uri.startsWith("/api/") || "OPTIONS".equalsIgnoreCase(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        final String path = request.getRequestURI();

        // 符合白名單的 API 直接放行（不做 JWT 解析）
        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 解析 Bearer Token
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            try {
                String username = jwtUtil.validateTokenAndGetUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // 驗證失敗就清空，交由後續授權規則決定是否 401/403
                SecurityContextHolder.clearContext();
            }
        }
        // 沒帶 token 也不在這裡擋，交由 SecurityConfig 的授權規則處理
        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String path) {
        for (String p : WHITELIST) {
            if (path.equals(p) || path.startsWith(p + "/")) {
                return true;
            }
        }
        return false;
    }
}