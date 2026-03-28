package com.hokhanh.ping_watch.ratelimit;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hokhanh.ping_watch.constant.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitRequestClassifier requestClassifier;
    private final RateLimitKeyResolver keyResolver;
    private final Bucket4jRateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitPolicyGroup policyGroup = requestClassifier.classify(request);
        if (policyGroup == RateLimitPolicyGroup.NONE) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = keyResolver.resolve(policyGroup, request);
        RateLimitResult result = rateLimitService.consume(policyGroup, key);

        if (result.consumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> payload = Map.of(
                "error", ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                "message", "Too many requests. Please try again later.",
                "retryAfterSeconds", result.retryAfterSeconds());

        objectMapper.writeValue(response.getWriter(), payload);
    }
}
