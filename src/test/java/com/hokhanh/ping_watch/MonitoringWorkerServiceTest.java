package com.hokhanh.ping_watch;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hokhanh.ping_watch.repository.MetricsRepository;
import com.hokhanh.ping_watch.repository.MonitoringConfigurationRepository;
import com.hokhanh.ping_watch.service.scheduler.MetricsStreamService;
import com.hokhanh.ping_watch.service.scheduler.MonitoringJobConsumer;
import com.hokhanh.ping_watch.service.scheduler.MonitoringJobPublisher;
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

    @Mock
    private MonitoringJobPublisher jobPublisher;

    @Mock
    private MetricsStreamService metricsStreamService;

    @Test
    void startConsumer_shouldUseTakeBasedConsumptionLoop() throws Exception {
        when(jobConsumer.take()).thenThrow(new InterruptedException("test-stop"));

        try {
            monitoringWorkerService.startConsumer();
            verify(jobConsumer, timeout(1000).atLeastOnce()).take();
        } finally {
            monitoringWorkerService.shutdownConsumer();
        }
    }
}
