package com.petpick.petpick.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class securityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                // 關閉 CSRF，方便用 API 工具測試
//                .csrf(csrf -> csrf.disable())
//
//                // 設定所有請求都允許（暫時用來測試）
//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().permitAll()
//                );
//
//        return http.build();

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/userlogin",
                                "/auth/register",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/styles.css",
                                "/memFunction.js",
                                "/login",      // 放行登入頁
                                "/index"       // 放行首頁（如果要公開）
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/userlogin") // 登入頁面改為 /login
                        .defaultSuccessUrl("/index", true)
                        .permitAll()
                )
                .logout(logout -> logout.permitAll());

        return http.build();
    }
}