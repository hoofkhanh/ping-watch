package com.hokhanh.ping_watch.ratelimit;

public record RateLimitResult(
        boolean consumed,
        long remainingTokens,
        long retryAfterSeconds) {

}
