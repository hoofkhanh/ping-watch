package com.hokhanh.ping_watch.service.scheduler;

import java.time.Instant;
import java.util.UUID;

public record MonitoringJob(
        UUID monitoringConfigurationId,
        Instant scheduledAt,
        String jobKey) {

}
