package com.hokhanh.ping_watch.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record MonitoringMetricItemResponse(
        UUID id,
        int statusCode,
        String statusName,
        double responseTime,
        LocalDateTime timestamp,
        boolean isSuccessful,
        String jobKey) {
}
