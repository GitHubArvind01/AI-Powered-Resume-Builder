package com.resumeai.resume_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

/**
 * JWT utility for parsing and validating JWT tokens
 * Extracts user information from JWT claims
 */
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String JWT_SECRET;

    @Value("${app.jwt.expiration-ms}")
    private long JWT_EXPIRATION_MS;

    private Key signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    /**
     * Extract email from JWT token
     *
     * @param token JWT token
     * @return Email extracted from token subject
     */
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Extract user ID from JWT token claims
     *
     * @param token JWT token
     * @return User ID from claims
     */
    public Long extractUserId(String token) {
        try {
            Object userId = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("userId");

            if (userId != null) {
                return Long.valueOf(userId.toString());
            }
            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token: " + e.getMessage());
        }
    }

    /**
     * Validate JWT token
     *
     * @param token JWT token to validate
     * @return true if token is valid and not expired
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if token is expired
     *
     * @param token JWT token
     * @return true if token is expired
     */
    public boolean isTokenExpired(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .before(new Date());
    }

    /**
     * Extract JWT token from Authorization header
     * Expected format: "Bearer <token>"
     *
     * @param authHeader Authorization header value
     * @return JWT token without "Bearer " prefix
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid authorization header format");
    }
}

