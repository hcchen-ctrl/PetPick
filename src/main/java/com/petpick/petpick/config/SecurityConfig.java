// SecurityConfig.java
package com.petpick.petpick.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                // ★ 只有第三方直接 POST 回來/不帶 CSRF token 的端點需要忽略 CSRF
                                .csrf(csrf -> csrf.ignoringRequestMatchers(
                                                new AntPathRequestMatcher("/payment/v2/result"),
                                                new AntPathRequestMatcher("/payment/v2/result/**"),
                                                new AntPathRequestMatcher("/payment/result"), // ★ 相容端點
                                                new AntPathRequestMatcher("/payment/result/**"), // ★ 相容端點
                                                new AntPathRequestMatcher("/api/pay/**"),
                                                new AntPathRequestMatcher("/api/logistics/**"),
                                                new AntPathRequestMatcher("/api/orders/**"),
                                                new AntPathRequestMatcher("/api/cart/**"),
                                                new AntPathRequestMatcher("/api/order-details/**")
                                // 若仍保留舊相容端點再打開下面兩行
                                // , new AntPathRequestMatcher("/payment/result"),
                                // new AntPathRequestMatcher("/payment/result/**")
                                ))
                                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                                .authorizeHttpRequests(auth -> auth
                                                // CORS Preflight
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                // 靜態頁與資源
                                                .requestMatchers("/", "/index.html", "/success.html", "/fail.html",
                                                                "/cart.html", "/order.html", "/orderDetail.html",
                                                                "/payment/result", "/payment/result/**",
                                                                "/css/**", "/js/**", "/images/**", "/figure/**",
                                                                "/webjars/**",
                                                                "/api/admin/orders/**", "/api/admin/**")
                                                .permitAll()
                                                // 金流/物流回呼與開放 API
                                                .requestMatchers("/payment/v2/result", "/payment/v2/result/**",
                                                                "/payment/result", "/payment/result/**")
                                                .permitAll()
                                                // 若仍保留舊相容端點再放行
                                                // .requestMatchers("/payment/result", "/payment/result/**").permitAll()
                                                .requestMatchers("/api/pay/**", "/api/logistics/**").permitAll()
                                                .requestMatchers("/api/cart/**", "/api/orders/**").permitAll()
                                                .requestMatchers("/api/order-details/**").permitAll()
                                                .requestMatchers("/api/admin/orders/**").permitAll()
                                                .requestMatchers("/api/admin/**").permitAll()
                                                // 其他按需調整；目前全開
                                                .anyRequest().permitAll());

                return http.build();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration cfg = new CorsConfiguration();
                cfg.setAllowedOriginPatterns(List.of("*"));
                cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                cfg.setAllowedHeaders(List.of("Content-Type", "X-Demo-UserId", "Authorization"));
                cfg.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", cfg);
                return source;
        }

        }

