package com.resumeai.auth.service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.resumeai.auth.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {	
	@Value("${app.jwt.secret}")
	private String SECRET;
	@Value("${app.jwt.expiration-ms}")
	private long expirationTime;
		
	private Key signingKey;
	
	@PostConstruct
	public void init() {
		this.signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
	}
	
	public String generateToken(User user) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("userId", user.getId());
		claims.put("role", user.getRole());
		claims.put("subscriptionPlan", user.getSubscriptionPlan());
		return Jwts.builder()
				.setClaims(claims)
				.setSubject(user.getEmail())
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis()+expirationTime))
				.signWith(signingKey, SignatureAlgorithm.HS256)
				.compact();
	}
	
	public String extractEmail(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(signingKey).build()
				.parseClaimsJws(token)
				.getBody()
				.getSubject();
	}
	
	public boolean validateToken(String token, String email) {
		return extractEmail(token).equals(email) && !isTokenExpired(token);
	}
	
	public boolean isTokenExpired(String token) {
		return Jwts.parserBuilder().setSigningKey(signingKey).build()
				.parseClaimsJws(token).getBody().getExpiration().before(new Date());
	}

	public Claims extractAllClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(signingKey)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	public String extractRole(String token) {
		Object role = extractAllClaims(token).get("role");
		return role == null ? null : role.toString();
	}
}
