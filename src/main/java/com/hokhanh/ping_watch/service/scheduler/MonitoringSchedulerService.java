package com.hokhanh.ping_watch.service.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hokhanh.ping_watch.model.MonitoringConfiguration;
import com.hokhanh.ping_watch.repository.MonitoringConfigurationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringSchedulerService {
    private final MonitoringConfigurationRepository monitoringConfigurationRepository;
    private final MonitoringRunStateService monitoringRunStateService;
    private final MonitoringJobQueue monitoringJobQueue;

    @Scheduled(fixedDelay = 1000)
    public void scheduleMonitoringJobs() {
        Instant now = Instant.now();

        for (UUID monitoringConfigurationId : monitoringRunStateService.getActiveConfigurationIds()) {
            monitoringConfigurationRepository.findById(monitoringConfigurationId)
                    .ifPresentOrElse(
                            monitoringConfiguration -> maybeEnqueueJob(monitoringConfiguration, now),
                            () -> monitoringRunStateService.stop(monitoringConfigurationId));
        }
    }

    private void maybeEnqueueJob(MonitoringConfiguration monitoringConfiguration, Instant now) {
        UUID configurationId = monitoringConfiguration.getId();
        Instant lastQueuedAt = monitoringRunStateService.getLastQueuedAt(configurationId);

        long intervalMillis = (long) (monitoringConfiguration.getInterval() * 1000);
        if (intervalMillis <= 0) {
            intervalMillis = 1000;
        }

        if (Duration.between(lastQueuedAt, now).toMillis() < intervalMillis) {
            return;
        }

        monitoringJobQueue.enqueue(new MonitoringJob(configurationId, now));
        monitoringRunStateService.markQueued(configurationId, now);

        log.info("Enqueued monitoring job for configurationId={}", configurationId);
    }
}
