package com.petpick.petpick.config;

import java.io.IOException;
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
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedDoubleSlash(true);
        return firewall;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 停用 CSRF，因為 JWT 是無狀態認證
        http.csrf(csrf -> csrf.disable());

        // 啟用 CORS
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // ✅ 停用預設的 form login 和 http basic
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());

        // ✅ 設定為無狀態會話
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // API 權限設定 - ⚠️ 順序很重要！更具體的路徑要放在前面
        http.authorizeHttpRequests(auth -> auth
                // 公開可存取的靜態資源和頁面
                .requestMatchers(
                        "/loginpage.html",
                        "/register",
                        "/",
                        "/index.html",
                        "/login/rename.html",
                        "/**.js",
                        "/**.css",
                        "/images/**",
                        "/styles.css",
                        "/chatroom.css")
                .permitAll()
                .requestMatchers("/ws/**").permitAll()

                // ✅ 認證相關 API
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

                // ✅ 商品相關 API
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                // ✅ 購物車相關 API
                .requestMatchers(HttpMethod.GET, "/api/cart/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/cart/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/cart/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/cart/**").authenticated()

                // ✅ 訂單相關 API
                .requestMatchers(HttpMethod.POST, "/api/orders/checkout").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").authenticated()

                // ✅ 物流相關 API
                .requestMatchers("/api/logistics/**").authenticated()
                .requestMatchers("/api/pay/**").authenticated()

                // ✅ 領養相關 API
                .requestMatchers("/api/kinds/**", "/api/shelters/**", "/api/ages/**", "/api/sexes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/adopts/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/adopts").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/adopts/*/apply").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/posts/*/cancel", "/api/posts/*/hold", "/api/posts/*/close")
                .authenticated()

                // ✅ 其他通用 API（移到最後，避免覆蓋上面的具體配置）
                .requestMatchers("/api/user/**").authenticated()

                // ✅ WebSocket 放行
                .requestMatchers("/ws/**").permitAll()

                // ✅ 所有其他 API 請求都需要認證（最後的兜底）
                .requestMatchers("/api/**").authenticated()

                // 其他請求（非 API）需要認證
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
                        } catch (IOException e) {
                            System.err.println("無法寫入錯誤回應: " + e.getMessage());
                        }
                    } else {
                        // 非 API 請求才重定向
                        try {
                            response.sendRedirect("/loginpage.html");
                        } catch (IOException e) {
                            System.err.println("重定向失敗: " + e.getMessage());
                        }
                    }
                })
                .accessDeniedHandler(myAccessDeniedHandler));

        // ✅ 確保JWT過濾器在最前面
        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ✅ 修正的 CORS 設定 Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ 使用 allowedOriginPatterns 而不是 allowedOrigins
        configuration.setAllowedOriginPatterns(List.of("http://localhost:5173"));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // 允許攜帶認證資訊

        // 設定預檢請求的快取時間
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
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