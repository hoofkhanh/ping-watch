package com.hokhanh.ping_watch.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RateLimitRequestClassifierTest {

    private final RateLimitRequestClassifier classifier = new RateLimitRequestClassifier();

    @Test
    void classify_shouldReturnAuth_forAuthEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users/login");

        RateLimitPolicyGroup result = classifier.classify(request);

        assertEquals(RateLimitPolicyGroup.AUTH, result);
    }

    @Test
    void classify_shouldReturnMonitoring_forMonitoringEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/monitoring-configurations");

        RateLimitPolicyGroup result = classifier.classify(request);

        assertEquals(RateLimitPolicyGroup.MONITORING, result);
    }

    @Test
    void classify_shouldReturnNone_forNonConfiguredEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");

        RateLimitPolicyGroup result = classifier.classify(request);

        assertEquals(RateLimitPolicyGroup.NONE, result);
    }
}
