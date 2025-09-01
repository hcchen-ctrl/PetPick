package tw.petpick.petpick.config;

import java.util.List;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration conf) throws Exception {
    return conf.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

  // ★ CORS 設定（允許 5173）
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOriginPatterns(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
    cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .cors(withDefaults())                            // ★ 開啟 CORS
      .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
      .authorizeHttpRequests(authz -> authz
        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
        .requestMatchers("/uploads/**").permitAll()

        // auth 開放
        .requestMatchers("/api/auth/login", "/api/auth/logout",
                         "/api/auth/status", "/api/auth/register",
                         "/api/auth/check-email", "/api/auth/verify-email",
                         "/api/auth/dev-code").permitAll()

        .requestMatchers("/", "/*.html", "/favicon.ico", "/manifest.json").permitAll()

        // 公開查詢的 API
        .requestMatchers(HttpMethod.GET,
            "/api/adopts", "/api/adopts/**",
            "/api/pets", "/api/pets/**",
            "/api/shelters", "/api/kinds", "/api/sexes", "/api/ages"
        ).permitAll()

        // 你原本的
        .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/gov/**").permitAll()

        // 後台
        .requestMatchers("/api/admin/**").hasRole("ADMIN")

        .anyRequest().authenticated()
      )

          // ★ 新增：未登入時回 401 + JSON（而不是純字串）
        .exceptionHandling(e -> e.authenticationEntryPoint((req, resp, ex) -> {
          resp.setStatus(401);
          resp.setContentType("application/json;charset=UTF-8");
          resp.getWriter().write("{\"code\":\"NOT_LOGIN\"}");
        }))

      .formLogin(form -> form.loginPage("/login.html").permitAll())
      .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login.html"));

    // 若仍遇到部分瀏覽器 preflight 被擋，可再加：
    // .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll());

    return http.build();
  }
}
