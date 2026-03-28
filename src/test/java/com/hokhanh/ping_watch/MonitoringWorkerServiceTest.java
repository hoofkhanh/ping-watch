package com.hokhanh.ping_watch;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hokhanh.ping_watch.repository.MetricsRepository;
import com.hokhanh.ping_watch.repository.MonitoringConfigurationRepository;
import com.hokhanh.ping_watch.service.scheduler.MonitoringJob;
import com.hokhanh.ping_watch.service.scheduler.MonitoringJobConsumer;
import com.hokhanh.ping_watch.service.scheduler.MonitoringWorkerService;

@ExtendWith(MockitoExtension.class)
class MonitoringWorkerServiceTest {

    @InjectMocks
    private MonitoringWorkerService monitoringWorkerService;

    @Mock
    private MonitoringJobConsumer jobConsumer;

    @Mock
    private MonitoringConfigurationRepository monitoringConfigurationRepository;

    @Mock
    private MetricsRepository metricsRepository;

    @Test
    void consumeAndProcessJobs_shouldSkipWhenJobKeyAlreadyProcessed() {
        MonitoringJob job = new MonitoringJob(UUID.randomUUID(), java.time.Instant.now(), "job-key-1");

        when(jobConsumer.poll()).thenReturn(job).thenReturn(null);
        when(metricsRepository.existsByJobKey("job-key-1")).thenReturn(true);

        monitoringWorkerService.consumeAndProcessJobs();

        verify(metricsRepository).existsByJobKey("job-key-1");
        verify(monitoringConfigurationRepository, never()).findById(job.monitoringConfigurationId());
    }
}
