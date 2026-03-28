package com.hokhanh.ping_watch.ratelimit;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnExpression("'${app.role:all}' == 'api' || '${app.role:all}' == 'all'")
@RequiredArgsConstructor
public class Bucket4jRateLimitService {
    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties properties;

    public RateLimitResult consume(RateLimitPolicyGroup group, String key) {
        BucketConfiguration configuration = buildConfiguration(group);

        Bucket bucket = proxyManager.builder()
                .build(key, () -> configuration);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return new RateLimitResult(true, probe.getRemainingTokens(), 0);
        }

        long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        return new RateLimitResult(false, probe.getRemainingTokens(), retryAfterSeconds);
    }

    private BucketConfiguration buildConfiguration(RateLimitPolicyGroup group) {
        RateLimitProperties.Policy policy = switch (group) {
            case AUTH -> properties.getAuth();
            case MONITORING -> properties.getMonitoring();
            case NONE -> throw new IllegalArgumentException("No rate-limit policy for NONE group");
        };

        Bandwidth limit = Bandwidth.classic(
                policy.getCapacity(),
                Refill.intervally(policy.getRefillTokens(), Duration.ofSeconds(policy.getRefillSeconds())));

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }
}
