// SecurityConfig.java
package com.petpick.petpick.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 若前端走不同網域(例如 ngrok)，需要打開 CORS
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/pay/ecpay/return",
                        "/api/pay/ecpay/result",
                        "/payment/result", "/payment/result/**",
                        "/api/logistics/**",
                        "/api/cart/**",
                        "/api/orders/**"
                ).permitAll()
                .anyRequest().permitAll()
                )
                // 忽略這些路徑的 CSRF（綠界回拋 & 購物車 API）
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                new AntPathRequestMatcher("/api/pay/ecpay/return"),
                new AntPathRequestMatcher("/api/pay/ecpay/result"),
                new AntPathRequestMatcher("/payment/result"),
                new AntPathRequestMatcher("/payment/result/**"),
                new AntPathRequestMatcher("/api/logistics/**"),
                new AntPathRequestMatcher("/api/cart/**"),
                new AntPathRequestMatcher("/api/orders/**")
        ));

        return http.build();
    }
}
