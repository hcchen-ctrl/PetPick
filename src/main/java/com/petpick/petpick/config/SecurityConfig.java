package com.petpick.petpick.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http)
      throws Exception {
    http
      // 只設定一次：開發環境先關 CSRF，避免非 GET 被擋
      .csrf(csrf -> csrf.disable())
      .cors(Customizer.withDefaults())
      .headers(h -> h.frameOptions(f -> f.sameOrigin()))
      .authorizeHttpRequests(auth -> auth
        // 放行預檢
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

        // 靜態與頁面
        .requestMatchers(
          "/", "/index.html", "/success.html", "/fail.html",
          "/cart.html", "/order.html", "/orderDetail.html",
          "/css/**", "/js/**", "/images/**", "/figure/**", "/webjars/**"
        ).permitAll()

        // 金流/物流回拋
        .requestMatchers("/payment/v2/result", "/payment/v2/result/**",
                         "/payment/result", "/payment/result/**").permitAll()

        // 目前對外開放的 API（含後台；上線前要再收緊）
        .requestMatchers("/api/pay/**",
                         "/api/logistics/**",
                         "/api/cart/**",
                         "/api/orders/**",
                         "/api/order-details/**",
                         "/api/admin/**",
                         "/api/products/**").permitAll()

        // 其他也放行（開發用）
        .anyRequest().permitAll()
      );

    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    // 若需要帶 cookie/認證資訊，建議改成固定來源，如 http://localhost:5173
    cfg.setAllowedOriginPatterns(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
    // 這裡一定要包含 PATCH
    cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of("Content-Type","X-Demo-UserId","X-Demo-Role","Authorization","X-Requested-With"));
    cfg.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}