package com.hokhanh.ping_watch.service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public interface JwtService {
    String generateToken(UUID userId, boolean isAccessToken);
	
	String buildToken(Map<String, Object> extraClaims, UUID userId, long expiration);
	
	Date extractExpiration(String token);
	
	boolean isRefreshTokenType(String token);
	
	String extractSubject(String token);
	
	Key getSignInKey();
}
