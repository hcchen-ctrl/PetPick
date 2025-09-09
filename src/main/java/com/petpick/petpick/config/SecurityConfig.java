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
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
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

    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private MyAccessDeniedHandler myAccessDeniedHandler;
    @Autowired
    @Lazy
    private CustomOAuth2UserService customOAuth2UserService;
    @Autowired
    private JwtUtil jwtUtil;

    @Bean
    public HttpFirewall allowUrlEncodedDoubleSlashHttpFirewall() {
        StrictHttpFirewall fw = new StrictHttpFirewall();
        fw.setAllowUrlEncodedDoubleSlash(true);
        return fw;
    }

    // ✅ 忽略純靜態資源（完全不進 Security/不會被 JWT Filter 影響）
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
            "/adopt/uploads/**",   // 靜態上傳圖片
            "/images/**",
            "/css/**",
            "/js/**",
            "/favicon.ico"
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // API only：無狀態，關 CSRF / 表單 / Basic
        http.csrf(csrf -> csrf.disable());
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());

        // ✅ 設定為無狀態會話
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

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
                        )
                .permitAll()
                .requestMatchers("/ws/**").permitAll()
                // 領養與回報專案的靜態圖
                .requestMatchers("/adopt/feedback/**",
                                "/adopt/uploads/**",
                                "/uploads/**" 
                                ).permitAll()
                // ===== 認證 & 公開 API =====
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()

                // ✅ 用戶相關 API - 新增這個重要區塊！
                .requestMatchers(HttpMethod.GET, "/api/users/avatar/**").permitAll() // 頭像可公開存取
                .requestMatchers(HttpMethod.GET, "/api/users/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/users/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").authenticated()

                // ✅ 任務擁有者相關 API
                .requestMatchers("/api/owners/**").authenticated()

                // ✅ 任務申請相關 API - 加入這個重要的配置！
                .requestMatchers("/api/applications/**").authenticated()
                .requestMatchers("/api/missionapplications/**").authenticated()

                // ✅ 任務相關 API
                .requestMatchers(HttpMethod.POST, "/api/missions/upload").permitAll()
                .requestMatchers("/api/missions/**").authenticated()
                .requestMatchers("/mission/missionsImg/**").permitAll()

                // ✅ 商品相關 API
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/cart/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/cart/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/cart/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/cart/**").authenticated()
                .requestMatchers("/api/owners/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/users/avatar/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/users/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/orders/checkout").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/missions/upload").permitAll()
                .requestMatchers("/api/applications/**").authenticated()
                .requestMatchers("/api/missionapplications/**").authenticated()
                .requestMatchers("/api/missions/**").authenticated()
                .requestMatchers("/api/kinds/**", "/api/shelters/**", "/api/ages/**", "/api/sexes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/adopts/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/adopts/uploads").authenticated()  // ← 新增
                .requestMatchers(HttpMethod.POST, "/api/adopts").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/adopts/*/apply").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/posts/*/cancel", "/api/posts/*/hold", "/api/posts/*/close")
                .authenticated()


                .requestMatchers("/api/user/**").authenticated()
                // 靜音 favicon（避免 401 噪音）
                .requestMatchers(HttpMethod.GET, "/favicon.ico").permitAll()

                // 其餘 API 需要登入
                .requestMatchers("/api/**").authenticated()

                .requestMatchers("/error").permitAll()

                .requestMatchers("/error").permitAll()

                .anyRequest().authenticated());

        // ✅ 修正異常處理 - 確保 API 請求不會被重定向
        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    String requestURI = request.getRequestURI();

                    System.out.println(
                            "🔐 認證失敗: " + request.getMethod() + " " + requestURI + " - " + authException.getMessage());
                    System.out.println("🔍 Auth Header: " + request.getHeader("Authorization"));

                    // ✅ 強制所有 /api/ 路徑都返回 JSON 錯誤，絕不重定向
                    if (requestURI.startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.setHeader("Cache-Control", "no-cache");
                        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
                        response.setHeader("Access-Control-Allow-Credentials", "true");

                        try {
                            response.getWriter().write("{" +
                                    "\"error\": \"Unauthorized\"," +
                                    "\"message\": \"JWT Token required\"," +
                                    "\"path\": \"" + requestURI + "\"," +
                                    "\"method\": \"" + request.getMethod() + "\"," +
                                    "\"timestamp\": \"" + java.time.Instant.now() + "\"" +
                                    "}");
                            response.getWriter().flush();
                        } catch (Exception e) {
                            System.err.println("無法寫入錯誤回應: " + e.getMessage());
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{" +
                                "\"error\": \"Unauthorized\"," +
                                "\"message\": \"Login required\"," +
                                "\"path\": \"" + requestURI + "\"," +
                                "\"method\": \"" + request.getMethod() + "\"," +
                                "\"timestamp\": \"" + java.time.Instant.now() + "\"" +
                                "}");
                        response.getWriter().flush();
                    }
                })
                .accessDeniedHandler(myAccessDeniedHandler));

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
                "https://payment.ecpay.com.tw"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}