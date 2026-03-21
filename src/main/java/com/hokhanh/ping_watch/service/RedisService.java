package com.hokhanh.ping_watch.service;

import java.time.Duration;


public interface RedisService {
	void set(String key, Object value, Duration ttl);
	
	<T> T get(String key, Class<T> clazz);
	
    void delete(String key);
}
