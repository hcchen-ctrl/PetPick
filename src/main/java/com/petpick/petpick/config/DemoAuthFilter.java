package com.petpick.petpick.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DemoAuthFilter extends OncePerRequestFilter {

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // 只處理 API 路徑；靜態資源放過
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      String uid  = request.getHeader("X-Demo-UserId");           // 例如 "1"
      String role = request.getHeader("X-Demo-Role");              // "ADMIN" or "USER"
      if (uid != null && !uid.isBlank()) {
        List<SimpleGrantedAuthority> auths = new ArrayList<>();
        auths.add(new SimpleGrantedAuthority("ROLE_USER"));
        if ("ADMIN".equalsIgnoreCase(String.valueOf(role).toUpperCase(Locale.ROOT))) {
          auths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        var auth = new UsernamePasswordAuthenticationToken("demo:"+uid, "N/A", auths);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    filterChain.doFilter(request, response);
  }
}