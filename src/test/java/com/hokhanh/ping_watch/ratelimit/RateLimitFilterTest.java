package com.hokhanh.ping_watch.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitRequestClassifier classifier;

    @Mock
    private RateLimitKeyResolver keyResolver;

    @Mock
    private Bucket4jRateLimitService rateLimitService;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        filter = new RateLimitFilter(
                classifier,
                keyResolver,
                rateLimitService,
                properties);
    }

    @Test
    void doFilterInternal_shouldPassThrough_whenTokenConsumed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/monitoring-configurations");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(classifier.classify(request)).thenReturn(RateLimitPolicyGroup.MONITORING);
        when(keyResolver.resolve(RateLimitPolicyGroup.MONITORING, request)).thenReturn("monitoring:user:u1");
        when(rateLimitService.consume(RateLimitPolicyGroup.MONITORING, "monitoring:user:u1"))
                .thenReturn(new RateLimitResult(true, 59, 0));

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("59", response.getHeader("X-RateLimit-Remaining"));
        verify(rateLimitService).consume(RateLimitPolicyGroup.MONITORING, "monitoring:user:u1");
    }

    @Test
    void doFilterInternal_shouldReturn429_whenTokenRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(classifier.classify(request)).thenReturn(RateLimitPolicyGroup.AUTH);
        when(keyResolver.resolve(RateLimitPolicyGroup.AUTH, request)).thenReturn("auth:ip:1.1.1.1");
        when(rateLimitService.consume(RateLimitPolicyGroup.AUTH, "auth:ip:1.1.1.1"))
                .thenReturn(new RateLimitResult(false, 0, 5));

        filter.doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        assertEquals("5", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));
    }
}
