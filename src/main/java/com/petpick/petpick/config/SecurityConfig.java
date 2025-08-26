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
        tokenRepository.setCookieName("XSRF-TOKEN"); // 確保 cookie 名稱

        http.csrf(csrf -> csrf
                .ignoringRequestMatchers("/register", "/api/missions/upload","/api/applications/**")
                .csrfTokenRepository(tokenRepository)
        );

        // 表單提交
        http.formLogin(form -> form
                // loginpage.html 表單 action 內容

                // 自定義登入頁面
                .loginPage("/loginpage")
                .loginProcessingUrl("/login")
                // 登入成功之後要造訪的頁面
                .defaultSuccessUrl("/", true)  // welcome 頁面
                // 登入失敗後要造訪的頁面
                .failureUrl("/loginpage?error=true")        );

        // ✅ Google OAuth2 登入設定
        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/loginpage") // 使用同一個登入頁
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService) // 自訂 OAuth2UserService
                )
                .defaultSuccessUrl("/", true) // Google 登入成功也導向 /
                .failureUrl("/loginpage?error=true")    // 登入失敗後要造訪的頁面
        );

        // 授權認證
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/css/**", "/js/**", "/images/**","/styles.css","/adopt/**","/shop/**","/memFunction.js", "/favicon.ico").permitAll()
                .requestMatchers("/index","/api/**","/gov-list-page","/adopt-list","/shop/commodity","/loginpage", "/register").permitAll() // ⬅ 加上 /register
                .requestMatchers("/rename").authenticated()//登入後才可以進入修改頁面
                .requestMatchers("/api/applications/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/posts/**").hasRole("ADMIN")
                .requestMatchers("/api/posts/**").hasRole("ADMIN") // 這裡也建議加
                .requestMatchers("/api/**").permitAll() // ⬅ 放在最後

                .requestMatchers("/managersIndex").authenticated()//登入後才可以進入修改頁面
                .requestMatchers("/adminpage").hasRole("ADMIN")
                .requestMatchers("/managerpage").hasRole("MANAGER")
                .requestMatchers("/employeepage").hasAnyRole("MANAGER", "EMPLOYEE")

                .requestMatchers("/").authenticated()
                .anyRequest().authenticated()
        );


//         http.csrf(csrf -> csrf.disable()); // 關閉 csrf 防護

        // 登出
        http.logout(logout -> logout
                .deleteCookies("JSESSIONID")
                .logoutSuccessUrl("/loginpage")
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // 可以使用任何的 HTTP 方法登出
        );

        // 異常處理
        http.exceptionHandling(exception -> exception
                //.accessDeniedPage("/異常處理頁面")  // 請自行撰寫
                .accessDeniedHandler(myAccessDeniedHandler)
        );

        // 勿忘我（remember-me）
        http.rememberMe(remember -> remember
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(60) // 通常都會大於 session timeout 的時間
        );

        return http.build();
    }

    // 注意！規定！要建立密碼演算的實例
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
