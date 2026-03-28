package com.hokhanh.ping_watch.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private Policy auth = new Policy(10, 10, 60);
    private Policy monitoring = new Policy(60, 60, 60);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Policy getAuth() {
        return auth;
    }

    public void setAuth(Policy auth) {
        this.auth = auth;
    }

    public Policy getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(Policy monitoring) {
        this.monitoring = monitoring;
    }

    public static class Policy {
        private long capacity;
        private long refillTokens;
        private long refillSeconds;

        public Policy() {
        }

        public Policy(long capacity, long refillTokens, long refillSeconds) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillSeconds = refillSeconds;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(long refillTokens) {
            this.refillTokens = refillTokens;
        }

        public long getRefillSeconds() {
            return refillSeconds;
        }

        public void setRefillSeconds(long refillSeconds) {
            this.refillSeconds = refillSeconds;
        }
    }
}
