package com.hokhanh.ping_watch.service.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

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
public class MonitoringSchedulerService {
    private final MonitoringConfigurationRepository monitoringConfigurationRepository;
    private final MonitoringJobPublisher jobPublisher;

    @Scheduled(fixedDelayString = "${app.scheduler.fixed-delay-ms:15000}")
    public void scheduleMonitoringJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<MonitoringConfiguration> dueConfigurations = monitoringConfigurationRepository
                .findByIsActiveTrueAndNextRunAtLessThanEqual(now);

        for (MonitoringConfiguration configuration : dueConfigurations) {
            String jobKey = configuration.getId() + ":" + now.toEpochSecond(ZoneOffset.UTC);
            MonitoringJob job = new MonitoringJob(configuration.getId(), now.toInstant(ZoneOffset.UTC), jobKey);

            jobPublisher.publish(job);

            configuration.setLastRunAt(now);
            configuration.setNextRunAt(now.plusSeconds((long) Math.max(1, configuration.getInterval())));
            monitoringConfigurationRepository.save(configuration);

            log.info("Published monitoring job configurationId={}, jobKey={}", configuration.getId(), jobKey);
        }
    }
}
