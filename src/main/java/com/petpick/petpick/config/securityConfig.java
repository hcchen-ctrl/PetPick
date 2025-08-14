package com.petpick.petpick.config;


import com.petpick.petpick.JwtUtil.JwtAuthenticationFilter;
import com.petpick.petpick.JwtUtil.JwtUtil;
import com.petpick.petpick.entity.userEntity;
import com.petpick.petpick.service.userService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class securityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
                   http
                    // 關閉 CSRF，方便用 API 工具測試
                    .csrf(csrf -> csrf.disable())

                    // 設定所有請求都允許（暫時用來測試）
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll()
                    );

            return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(userService userService) {
        return username -> {
            userEntity user = userService.findByAccountemail(username);
            if (user == null) {
                throw new UsernameNotFoundException("找不到使用者: " + username);
            }
            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getAccountemail())
                    .password(user.getPassword())
                    .authorities("USER")
                    .build();
        };
    }
}