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
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (JwtException e) {
            // 包含過期、格式錯誤、無效簽名等
            return null;
        }
    }
}