package com.petpick.petpick.config;

import com.petpick.petpick.JWT.JwtAuthenticationFilter;
import com.petpick.petpick.JWT.JwtUtil;
import com.petpick.petpick.handle.MyAccessDeniedHandler;
import com.petpick.petpick.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
        // CSRF 設定：由於您的應用程式使用 JWT 進行無狀態身分驗證，
        // CSRF 防護不再需要，且會與此架構產生衝突，因此將其停用。
        http.csrf(csrf -> csrf.disable());

        // API 權限設定
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
                        "/chatroom.css"
                ).permitAll()
                // 所有 /api/user/ 下的請求都需要身分驗證
                .requestMatchers("/api/user/**").authenticated()
                .anyRequest().authenticated()
        );

        // 異常處理（未登入、權限不足）
        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    String accept = request.getHeader("Accept");
                    if (accept != null && accept.contains("text/html")) {
                        response.sendRedirect("/loginpage.html");
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Unauthorized\"}");
                    }
                })
                .accessDeniedHandler(myAccessDeniedHandler)
        );

        // 登出設定
        http.logout(logout -> logout
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .logoutSuccessUrl("/loginpage.html")
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
        );

        // JWT Filter 加入到 Spring Security 過濾器鏈
        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
