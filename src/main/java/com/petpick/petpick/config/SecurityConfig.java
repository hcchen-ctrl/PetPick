package com.petpick.petpick.config;

import com.petpick.petpick.handle.MyAccessDeniedHandler;
import com.petpick.petpick.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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

    //暫時開放
    @Bean
    public HttpFirewall allowUrlEncodedDoubleSlashHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedDoubleSlash(true); // 允許 URL 中有 //
        return firewall;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 創建自定義的 CSRF token repository
        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokenRepository.setHeaderName("X-Csrf-Token");
        tokenRepository.setCookieName("XSRF-TOKEN");

        http.csrf(csrf -> csrf
                .ignoringRequestMatchers("/register", "/api/missions/upload", "/api/applications/**")
                .ignoringRequestMatchers("/api/posts/**")
                .csrfTokenRepository(tokenRepository)
        );

        // 表單提交
        http.formLogin(form -> form
                .loginPage("/loginpage")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/loginpage?error=true")
        );

        // Google OAuth2 登入設定
        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/loginpage")
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/", true)
                .failureUrl("/loginpage?error=true")
        );

        // ⚠️ 重要：授權認證順序修正 - 從最具體到最通用
        http.authorizeHttpRequests(authorize -> authorize
                // 靜態資源
                .requestMatchers("/css/**", "/js/**", "/images/**", "/styles.css", "/adopt/**", "/shop/**", "/memFunction.js", "/favicon.ico").permitAll()

                // 公開頁面
                .requestMatchers("/index", "/gov-list-page", "/adopt-list", "/shop/commodity", "/loginpage", "/register").permitAll()

                // ⚠️ 具體的管理員 API 路徑 - 必須放在最前面
                .requestMatchers(HttpMethod.GET, "/api/posts").hasRole("ADMIN")        // 審核中心列表
                .requestMatchers(HttpMethod.GET, "/api/posts/*").hasRole("ADMIN")      // 單個貼文詳情
                .requestMatchers(HttpMethod.PATCH, "/api/posts/*/status").hasRole("ADMIN")  // 審核狀態更新
                .requestMatchers(HttpMethod.PATCH, "/api/posts/*/hold").hasRole("ADMIN")    // 暫停/恢復
                .requestMatchers(HttpMethod.PATCH, "/api/posts/*/close").hasRole("ADMIN")   // 關閉貼文

                // 其他管理員專用 API
                .requestMatchers("/api/applications/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/posts/**").hasRole("ADMIN")

                // 用戶相關的 API - 需要登入但不限制角色
                .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()        // 創建貼文
                .requestMatchers(HttpMethod.GET, "/api/posts/my").authenticated()      // 我的貼文
                .requestMatchers(HttpMethod.PATCH, "/api/posts/*/cancel").authenticated() // 取消貼文

                // 通用 API 路徑 - 放在最後
                .requestMatchers("/api/**").permitAll()

                // 其他需要認證的頁面
                .requestMatchers("/rename").authenticated()
                .requestMatchers("/managersIndex").authenticated()
                .requestMatchers("/adminpage").hasRole("ADMIN")
                .requestMatchers("/managerpage").hasRole("MANAGER")
                .requestMatchers("/employeepage").hasAnyRole("MANAGER", "EMPLOYEE")

                .requestMatchers("/").authenticated()
                .anyRequest().authenticated()
        );

        // 登出
        http.logout(logout -> logout
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")  // 同時清除 CSRF token
                .logoutSuccessUrl("/loginpage")
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
        );

        // 異常處理
        http.exceptionHandling(exception -> exception
                .accessDeniedHandler(myAccessDeniedHandler)
        );

        // 勿忘我
        http.rememberMe(remember -> remember
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(60)
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}