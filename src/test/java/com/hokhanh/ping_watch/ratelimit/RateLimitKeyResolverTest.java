package com.hokhanh.ping_watch.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitKeyResolverTest {

    private final RateLimitKeyResolver resolver = new RateLimitKeyResolver();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolve_shouldUseAuthenticatedUserId_whenAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user-123",
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        String result = resolver.resolve(RateLimitPolicyGroup.MONITORING, request);

        assertEquals("monitoring:user:user-123", result);
    }

    @Test
    void resolve_shouldUseForwardedIp_whenUnauthenticated() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.10.10.10, 20.20.20.20");
        request.setRemoteAddr("127.0.0.1");

        String result = resolver.resolve(RateLimitPolicyGroup.AUTH, request);

        assertEquals("auth:ip:10.10.10.10", result);
    }

    @Test
    void resolve_shouldUseRemoteAddr_whenNoForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        String result = resolver.resolve(RateLimitPolicyGroup.AUTH, request);

        assertEquals("auth:ip:192.168.1.100", result);
    }
}
