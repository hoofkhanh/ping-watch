package com.hokhanh.ping_watch.service.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hokhanh.ping_watch.model.MonitoringConfiguration;
import com.hokhanh.ping_watch.repository.MonitoringConfigurationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnExpression("'${app.role:all}' == 'scheduler' || '${app.role:all}' == 'all'")
@RequiredArgsConstructor
// this class is responsible for publishing jobs into DelayQueue after
// restarting
// the DelayQueue is empty so we need to push jobs to this queue to make sure
// the monitoring process is not interrupted
public class MonitoringSchedulerService {
    private final MonitoringConfigurationRepository monitoringConfigurationRepository;
    private final MonitoringJobPublisher jobPublisher;

    @Scheduled(fixedDelayString = "${app.scheduler.reconcile-delay-ms:60000}")
    public void scheduleMonitoringJobs() {
        List<MonitoringConfiguration> activeConfigurations = monitoringConfigurationRepository.findByIsActiveTrue();

        for (MonitoringConfiguration configuration : activeConfigurations) {
            Instant runAt = configuration.getNextRunAt() == null
                    ? Instant.now()
                    : configuration.getNextRunAt().toInstant(ZoneOffset.UTC);
            MonitoringJob job = new MonitoringJob(
                    configuration.getId(),
                    runAt,
                    configuration.getScheduleVersion(),
                    toJobKey(configuration.getId(), configuration.getScheduleVersion(), runAt));

            jobPublisher.publish(job);
        }
    }

    private String toJobKey(UUID configurationId, long scheduleVersion, Instant runAt) {
        return configurationId + ":" + scheduleVersion + ":" + runAt.getEpochSecond();
    }
}
