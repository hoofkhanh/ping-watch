package com.hokhanh.ping_watch.ratelimit;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RateLimitRequestClassifier {

    public RateLimitPolicyGroup classify(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (isAuthPath(path)) {
            return RateLimitPolicyGroup.AUTH;
        }

        if (path != null && path.startsWith("/monitoring-configurations")) {
            return RateLimitPolicyGroup.MONITORING;
        }

        return RateLimitPolicyGroup.NONE;
    }

    private boolean isAuthPath(String path) {
        if (path == null) {
            return false;
        }

        return path.equals("/users/register")
                || path.equals("/users/confirm-otp")
                || path.equals("/users/login")
                || path.equals("/users/refresh-token");
    }
}
