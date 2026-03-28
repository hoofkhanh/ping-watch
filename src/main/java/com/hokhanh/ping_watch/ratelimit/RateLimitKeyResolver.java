package com.hokhanh.ping_watch.ratelimit;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
@ConditionalOnExpression("'${app.role:all}' == 'api' || '${app.role:all}' == 'all'")
public class RateLimitKeyResolver {

    public String resolve(RateLimitPolicyGroup group, HttpServletRequest request) {
        String prefix = group.name().toLowerCase();

        String authenticatedUserId = getAuthenticatedUserId();
        if (authenticatedUserId != null) {
            return prefix + ":user:" + authenticatedUserId;
        }

        String ip = extractClientIp(request);
        return prefix + ":ip:" + ip;
    }

    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            return null;
        }

        return String.valueOf(authentication.getPrincipal());
    }

    String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return Optional.ofNullable(request.getRemoteAddr()).orElse("unknown");
    }
}
