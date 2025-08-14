package com.petpick.petpick.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**")) // 前後端分離，API 先關 CSRF
                .cors(cors -> {
                }) // 啟用 CORS（下面會有 bean）
                .authorizeHttpRequests(auth -> auth
                // 靜態資源 & 頁面
                .requestMatchers("/", "/index.html", "/commodity.html", "/product.html",
                        "/css/**", "/js/**", "/images/**").permitAll()
                // 商品 API：GET 開放
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                //（如果購物車讀取也要開放 GET）
                .requestMatchers(HttpMethod.GET, "/api/cart/**").permitAll()
                // 其他先放行（你有需要可改成 authenticated()）
                .anyRequest().permitAll()
                );
        return http.build();
    }
}
