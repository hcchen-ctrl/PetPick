package com.petpick.petpick.JWT;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET_KEY = "ThisIsASecretKeyForJWTTokenThatShouldBeLongEnough123456";
    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 小時

    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // 產生 JWT
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 驗證 JWT 並回傳使用者名稱
    public String validateTokenAndGetUsername(String token) {
        try {
            String username = Jwts.parserBuilder()
                    .setSigningKey(key) // 這行一定要有，才能驗證簽名
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            System.out.println("✅ JWT 驗證成功, username = " + username);
            return username;
        } catch (Exception e) {
            System.out.println("❌ JWT 驗證失敗: " + e.getMessage());
            return null;
        }
    }




}