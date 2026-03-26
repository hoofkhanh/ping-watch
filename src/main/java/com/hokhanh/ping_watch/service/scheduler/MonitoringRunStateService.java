package com.hokhanh.ping_watch.service.scheduler;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MonitoringRunStateService {
    private final Set<UUID> activeConfigurationIds = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Instant> lastQueuedAtByConfiguration = new ConcurrentHashMap<>();

    public void start(UUID monitoringConfigurationId) {
        log.info("Starting monitoring for configuration: {}", monitoringConfigurationId);
        activeConfigurationIds.add(monitoringConfigurationId);
        lastQueuedAtByConfiguration.putIfAbsent(monitoringConfigurationId, Instant.EPOCH);
    }

    public void stop(UUID monitoringConfigurationId) {
        log.info("Stopping monitoring for configuration: {}", monitoringConfigurationId);
        activeConfigurationIds.remove(monitoringConfigurationId);
        lastQueuedAtByConfiguration.remove(monitoringConfigurationId);
    }

    public Set<UUID> getActiveConfigurationIds() {
        return Set.copyOf(activeConfigurationIds);
    }

    public Instant getLastQueuedAt(UUID monitoringConfigurationId) {
        return lastQueuedAtByConfiguration.getOrDefault(monitoringConfigurationId, Instant.EPOCH);
    }

    public void markQueued(UUID monitoringConfigurationId, Instant queuedAt) {
        lastQueuedAtByConfiguration.put(monitoringConfigurationId, queuedAt);
    }
}
