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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS：對應下面的 corsConfigurationSource()
            .cors(Customizer.withDefaults())

            // CSRF：只忽略第三方回拋與你方 JSON POST
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                new AntPathRequestMatcher("/api/pay/ecpay/**",   "POST"),
                new AntPathRequestMatcher("/api/logistics/**",   "POST"),
                new AntPathRequestMatcher("/api/orders/checkout","POST"),
                new AntPathRequestMatcher("/api/cart/**",        "POST")
            ))

            // 若有在 iframe 內嵌頁面，可開 sameOrigin
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))

            .authorizeHttpRequests(auth -> auth
                // 預檢請求一定放行
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 靜態資源 / 頁面
                .requestMatchers("/", "/index.html",
                        "/cart.html", "/order.html", "/orderDetail.html",
                        "/payment/result", "/payment/result/**",
                        "/css/**", "/js/**", "/images/**", "/figure/**", "/webjars/**")
                .permitAll()

                // API（測試階段先放行，上線再縮）
                .requestMatchers("/api/pay/ecpay/**").permitAll()
                .requestMatchers("/api/logistics/**").permitAll()
                .requestMatchers("/api/cart/**").permitAll()
                .requestMatchers("/api/orders/**").permitAll()

                // 其他（測試放行）
                .anyRequest().permitAll()
            );

        return http.build();
    }

    // CORS：測試模式允許全部；上線請改為你的前端網域
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
