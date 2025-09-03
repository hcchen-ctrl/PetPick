package com.petpick.petpick.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.petpick.petpick.JWT.JwtAuthenticationFilter;
import com.petpick.petpick.JWT.JwtUtil;
import com.petpick.petpick.handle.MyAccessDeniedHandler;
import com.petpick.petpick.service.CustomOAuth2UserService;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired private UserDetailsService userDetailsService;
    @Autowired private MyAccessDeniedHandler myAccessDeniedHandler;
    @Autowired @Lazy private CustomOAuth2UserService customOAuth2UserService;
    @Autowired private JwtUtil jwtUtil;

    @Bean
    public HttpFirewall allowUrlEncodedDoubleSlashHttpFirewall() {
        StrictHttpFirewall fw = new StrictHttpFirewall();
        fw.setAllowUrlEncodedDoubleSlash(true);
        return fw;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // API only：無狀態，關 CSRF / 表單 / Basic
        http.csrf(csrf -> csrf.disable());
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
            // CORS 預檢
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // ===== 綠界 / 物流（S2S & 導頁）=====
            .requestMatchers("/payment/**").permitAll() // OrderResultURL 進來會走這裡
            .requestMatchers(HttpMethod.POST, "/api/pay/ecpay/return").permitAll() // ReturnURL S2S
            .requestMatchers(
                "/api/logistics/home/reply",
                "/api/logistics/home/ecpay/reply",
                "/api/logistics/cvs/store-return",
                "/api/logistics/cvs/ecpay/create-return"
            ).permitAll()

            // ===== 認證 & 公開 API =====
            .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/users/avatar/**").permitAll()
            .requestMatchers("/api/kinds/**", "/api/shelters/**", "/api/ages/**", "/api/sexes/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/adopts/**").permitAll()

            // 靜音 favicon（避免 401 噪音）
            .requestMatchers(HttpMethod.GET, "/favicon.ico").permitAll()

            // 其餘 API 需要登入
            .requestMatchers("/api/**").authenticated()

            // 後端不再提供任何頁面/靜態資源：全部拒絕
            .anyRequest().denyAll()
        );

        // 統一錯誤處理：/api/** 回 JSON，其它回 404（不再重導 loginpage.html）
        http.exceptionHandling(ex -> ex
            .authenticationEntryPoint((req, res, e) -> {
                String uri = req.getRequestURI();
                if (uri.startsWith("/api/")) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    String origin = req.getHeader("Origin");
                    if (origin != null && !origin.isBlank()) {
                        res.setHeader("Access-Control-Allow-Origin", origin);
                        res.setHeader("Vary", "Origin");
                    }
                    res.setHeader("Access-Control-Allow-Credentials", "true");
                    res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"JWT Token required\",\"path\":\""+uri+"\"}");
                } else {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            })
            .accessDeniedHandler((req, res, e) -> {
                if (req.getRequestURI().startsWith("/api/")) {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json;charset=UTF-8");
                    String origin = req.getHeader("Origin");
                    if (origin != null && !origin.isBlank()) {
                        res.setHeader("Access-Control-Allow-Origin", origin);
                        res.setHeader("Vary", "Origin");
                    }
                    res.setHeader("Access-Control-Allow-Credentials", "true");
                    res.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                } else {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            })
        );

        // JWT 過濾器
        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS：允許前端與 ECPay
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
            "http://localhost:5173",
            "https://localhost:5173",
            "https://de6a509fbde7.ngrok-free.app",
            "https://payment-stage.ecpay.com.tw",
            "https://payment.ecpay.com.tw"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setExposedHeaders(List.of("Location"));
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}