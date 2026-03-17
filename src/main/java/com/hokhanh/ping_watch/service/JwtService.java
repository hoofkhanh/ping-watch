package com.hokhanh.ping_watch.service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JwtService {
	@Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration.refresh}")
    private long refreshExpiration;

	@Value("${jwt.expiration.access}")
    private long accessExpiration;

    public String generateToken(UUID userId, boolean isAccessToken){
		log.info("Generating {} token for userId: {}", isAccessToken ? "access" : "refresh", userId);

		Map<String, Object> claims = new HashMap<>();
		claims.put("type", isAccessToken ? "access" : "refresh");

		return Jwts.builder()
			.setClaims(claims)
			.setSubject(userId.toString())
			.setIssuedAt(new Date(System.currentTimeMillis()))
			.setExpiration(new Date(System.currentTimeMillis() + (isAccessToken ? accessExpiration : refreshExpiration)))
			.signWith(getSignInKey(), SignatureAlgorithm.HS256)
			.compact();
	}

	public boolean isTokenValid(String token){
		log.info("Validating token: {}", token);
		try {
	        Date extractedDate = extractAllClaims(token).getExpiration();
			return new Date().before(extractedDate);
	    } catch (ExpiredJwtException e) {
	        return false;
	    } catch (Exception e) {
	        return false; 
	    }
	}
	
	public boolean isRefreshTokenType(String token){
		log.info("Checking if token is refresh token: {}", token);
		try {
	        String type = extractAllClaims(token).get("type", String.class);
	        return "refresh".equals(type);
	    } catch (Exception e) {
	        return false; 
	    }
	}
	
	public String extractUserId(String token){
		log.info("Extracting userId from token: {}", token);
		try {
	        return extractAllClaims(token).getSubject();
	    } catch (ExpiredJwtException e) {
			log.info("Token expired while extracting userId: {}", token);
	        return null;
	    } catch (Exception e) {
			log.info("Error extracting userId from token: {}. Error: {}", token, e.getMessage());
	        return null;
	    }
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(getSignInKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}
	
	private Key getSignInKey(){
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
