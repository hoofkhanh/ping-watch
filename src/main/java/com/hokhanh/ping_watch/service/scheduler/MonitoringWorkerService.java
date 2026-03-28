package com.hokhanh.ping_watch.service.scheduler;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.hokhanh.ping_watch.model.HttpMethod;
import com.hokhanh.ping_watch.model.Metrics;
import com.hokhanh.ping_watch.model.MonitoringConfiguration;
import com.hokhanh.ping_watch.repository.MetricsRepository;
import com.hokhanh.ping_watch.repository.MonitoringConfigurationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnExpression("'${app.role:all}' == 'worker' || '${app.role:all}' == 'all'")
@RequiredArgsConstructor
public class MonitoringWorkerService {
    private final MonitoringJobConsumer jobConsumer;
    private final MonitoringConfigurationRepository monitoringConfigurationRepository;
    private final MetricsRepository metricsRepository;

    @Scheduled(fixedDelay = 300)
    public void consumeAndProcessJobs() {
        MonitoringJob monitoringJob;

        while ((monitoringJob = jobConsumer.poll()) != null) {
            processJob(monitoringJob);
        }
    }

    private void processJob(MonitoringJob monitoringJob) {
        if (metricsRepository.existsByJobKey(monitoringJob.jobKey())) {
            log.info("Skipping duplicated monitoring job with key={}", monitoringJob.jobKey());
            return;
        }

        log.info("Consuming monitoring job with key={}", monitoringJob.jobKey());

        monitoringConfigurationRepository.findById(monitoringJob.monitoringConfigurationId())
                .ifPresent(configuration -> callApiAndStoreMetrics(configuration, monitoringJob.jobKey()));
    }

    private void callApiAndStoreMetrics(MonitoringConfiguration monitoringConfiguration, String jobKey) {
        long startedAt = System.nanoTime();

        int statusCode = 0;
        String statusName = "REQUEST_ERROR";
        boolean successful = false;

        try {
            RestTemplate restTemplate = buildRestTemplate(monitoringConfiguration.getTimeout());
            ResponseEntity<String> response = restTemplate.exchange(
                    monitoringConfiguration.getUrl(),
                    toSpringHttpMethod(monitoringConfiguration.getHttpMethod()),
                    HttpEntity.EMPTY,
                    String.class);

            statusCode = response.getStatusCode().value();
            statusName = response.getStatusCode().toString();
            successful = response.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            log.warn("Monitoring check failed for configurationId={}, reason={}",
                    monitoringConfiguration.getId(),
                    ex.getMessage());
        }

        double responseTimeInSeconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;

        Metrics metrics = Metrics.builder()
                .statusCode(statusCode)
                .statusName(statusName)
                .responseTime(responseTimeInSeconds)
                .timestamp(LocalDateTime.now())
                .isSuccessful(successful)
                .jobKey(jobKey)
                .monitoringConfiguration(monitoringConfiguration)
                .build();

        try {
            metricsRepository.save(metrics);
        } catch (DataIntegrityViolationException ex) {
            log.info("Duplicate job key ignored: {}", jobKey);
        }
    }

    private RestTemplate buildRestTemplate(double timeoutInSeconds) {
        int timeoutMillis = (int) (Math.max(0.1, timeoutInSeconds) * 1000);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);

        return new RestTemplate(requestFactory);
    }

    private org.springframework.http.HttpMethod toSpringHttpMethod(HttpMethod httpMethod) {
        return switch (httpMethod) {
            case GET -> org.springframework.http.HttpMethod.GET;
            case POST -> org.springframework.http.HttpMethod.POST;
            case PUT -> org.springframework.http.HttpMethod.PUT;
            case PATCH -> org.springframework.http.HttpMethod.PATCH;
            case DELETE -> org.springframework.http.HttpMethod.DELETE;
        };
    }
}
