package com.hokhanh.ping_watch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hokhanh.ping_watch.model.HttpMethod;
import com.hokhanh.ping_watch.model.MonitoringConfiguration;
import com.hokhanh.ping_watch.model.User;
import com.hokhanh.ping_watch.repository.MonitoringConfigurationRepository;
import com.hokhanh.ping_watch.service.scheduler.MonitoringJob;
import com.hokhanh.ping_watch.service.scheduler.MonitoringJobPublisher;
import com.hokhanh.ping_watch.service.scheduler.MonitoringSchedulerService;

@ExtendWith(MockitoExtension.class)
class MonitoringSchedulerServiceTest {

    @InjectMocks
    private MonitoringSchedulerService monitoringSchedulerService;

    @Mock
    private MonitoringConfigurationRepository monitoringConfigurationRepository;

    @Mock
    private MonitoringJobPublisher jobPublisher;

    @Test
    void scheduleMonitoringJobs_shouldPublishAndUpdateDueConfigurations() {
        MonitoringConfiguration configuration = MonitoringConfiguration.builder()
                .id(UUID.randomUUID())
                .name("Google")
                .httpMethod(HttpMethod.GET)
                .url("https://google.com")
                .interval(10)
                .timeout(3)
                .isActive(true)
                .nextRunAt(LocalDateTime.now().minusSeconds(1))
                .user(new User())
                .build();

        when(monitoringConfigurationRepository.findByIsActiveTrueAndNextRunAtLessThanEqual(any()))
                .thenReturn(List.of(configuration));
        when(monitoringConfigurationRepository.save(any(MonitoringConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        monitoringSchedulerService.scheduleMonitoringJobs();

        verify(jobPublisher, times(1)).publish(any(MonitoringJob.class));
        verify(monitoringConfigurationRepository, times(1)).save(any(MonitoringConfiguration.class));
    }
}
