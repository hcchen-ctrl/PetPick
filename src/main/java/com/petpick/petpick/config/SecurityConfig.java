// src/main/java/com/petpick/petpick/config/SecurityConfig.java
package com.petpick.petpick.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        SecurityFilterChain filterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http)
                        throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                // ★ 忽略不帶 CSRF token 的端點（第三方回拋 & 你前端目前沒送 CSRF 的 API）
                                .csrf(csrf -> csrf.ignoringRequestMatchers(
                                                new AntPathRequestMatcher("/payment/v2/result"),
                                                new AntPathRequestMatcher("/payment/v2/result/**"),
                                                new AntPathRequestMatcher("/payment/result"),
                                                new AntPathRequestMatcher("/payment/result/**"),
                                                new AntPathRequestMatcher("/api/pay/**"),
                                                new AntPathRequestMatcher("/api/logistics/**"),
                                                new AntPathRequestMatcher("/api/orders/**"),
                                                new AntPathRequestMatcher("/api/cart/**"),
                                                new AntPathRequestMatcher("/api/order-details/**"),
                                                new AntPathRequestMatcher("/api/admin/**"),
                                                new AntPathRequestMatcher("/api/products/**") // ← 關鍵：忽略 admin API 的 CSRF
                                ))
                                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                                .authorizeHttpRequests(auth -> auth
                                                // CORS 預檢
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // 靜態頁與資源
                                                .requestMatchers(
                                                                "/", "/index.html", "/success.html", "/fail.html",
                                                                "/cart.html", "/order.html", "/orderDetail.html",
                                                                "/css/**", "/js/**", "/images/**", "/figure/**",
                                                                "/webjars/**")
                                                .permitAll()

                                                // 金流/物流回拋
                                                .requestMatchers("/payment/v2/result", "/payment/v2/result/**",
                                                                "/payment/result", "/payment/result/**")
                                                .permitAll()

                                                // 目前專案對外開放的 API（含 admin；若日後要上鎖再收緊）
                                                .requestMatchers("/api/pay/**",
                                                                "/api/logistics/**",
                                                                "/api/cart/**",
                                                                "/api/orders/**",
                                                                "/api/order-details/**",
                                                                "/api/admin/**",
                                                                "/api/products/**")
                                                .permitAll()

                                                // 其他全部放行（開發階段）
                                                .anyRequest().permitAll());

                return http.build();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration cfg = new CorsConfiguration();
                cfg.setAllowedOriginPatterns(List.of("*"));
                cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                // 如有用到 demo 權限標頭，一併放行
                cfg.setAllowedHeaders(List.of("Content-Type", "X-Demo-UserId", "X-Demo-Role", "Authorization"));
                cfg.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", cfg);
                return source;
        }
}