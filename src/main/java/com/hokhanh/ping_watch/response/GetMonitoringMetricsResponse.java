package com.hokhanh.ping_watch.response;

import java.time.LocalDateTime;
import java.util.List;

public record GetMonitoringMetricsResponse(
        List<MonitoringMetricItemResponse> items,
        MetricsSummary summary) {

    public record MetricsSummary(
            long totalChecks,
            long successCount,
            long failureCount,
            double successRate,
            double avgResponseTime,
            String latestStatus,
            LocalDateTime latestTimestamp) {
    }
}
