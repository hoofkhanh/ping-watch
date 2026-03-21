package com.hokhanh.ping_watch.service.impl;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hokhanh.ping_watch.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService{
    private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	public void set(String key, Object value, Duration ttl) {
        log.info("Setting Redis key: {} with TTL: {}", key, ttl);
	    redisTemplate.opsForValue().set(key, value, ttl);
	}
	
	public <T> T get(String key, Class<T> clazz) {
        log.info("Getting Redis key: {}", key);
	    Object value = redisTemplate.opsForValue().get(key);
	    if (value == null) return null;

	    return objectMapper.convertValue(value, clazz);
	}
	
	public void delete(String key) {
        log.info("Deleting Redis key: {}", key);
		redisTemplate.delete(key);
	}
}
