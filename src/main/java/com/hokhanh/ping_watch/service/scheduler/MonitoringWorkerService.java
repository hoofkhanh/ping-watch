package com.hokhanh.ping_watch.service.scheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.hokhanh.ping_watch.model.HttpMethod;
import com.hokhanh.ping_watch.model.Metrics;
import com.hokhanh.ping_watch.model.MonitoringConfiguration;
import com.hokhanh.ping_watch.repository.MetricsRepository;
import com.hokhanh.ping_watch.repository.MonitoringConfigurationRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnExpression("'${app.role:all}' == 'worker' || '${app.role:all}' == 'all'")
@RequiredArgsConstructor
public class MonitoringWorkerService {
    private static final long MIN_INTERVAL_MILLIS = 3000L;

    private final MonitoringJobConsumer jobConsumer;
    private final MonitoringJobPublisher jobPublisher;
    private final MonitoringConfigurationRepository monitoringConfigurationRepository;
    private final MetricsRepository metricsRepository;
    private final MetricsStreamService metricsStreamService;

    @Value("${app.worker.pool-size:4}")
    private int workerPoolSize = 4;

    private volatile boolean running = true;
    private Thread consumerThread;
    private ExecutorService executorService;
    private Semaphore inFlightSemaphore;

    @PostConstruct
    public void startConsumer() {
        executorService = Executors.newFixedThreadPool(workerPoolSize);
        inFlightSemaphore = new Semaphore(workerPoolSize);
        consumerThread = new Thread(this::consumeLoop, "monitoring-job-consumer");
        consumerThread.setUncaughtExceptionHandler(
                (thread, throwable) -> log.error("Consumer thread crashed. thread={}", thread.getName(), throwable));
        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("Monitoring worker started with pool size={}", workerPoolSize);
    }

    @PreDestroy
    public void shutdownConsumer() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void consumeLoop() {
        while (running) {
            try {
                MonitoringJob monitoringJob = jobConsumer.take();
                long queueLagMillis = Math.max(0, Instant.now().toEpochMilli() - monitoringJob.runAt().toEpochMilli());
                log.info("Dequeued job key={}, lag={}ms", monitoringJob.jobKey(), queueLagMillis);
                inFlightSemaphore.acquire();
                executorService.execute(() -> processWithRelease(monitoringJob));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                log.error("Unexpected worker consumer error", ex);
            }
        }
    }

    private void processWithRelease(MonitoringJob monitoringJob) {
        try {
            processJob(monitoringJob);
        } finally {
            inFlightSemaphore.release();
        }
    }

    private void processJob(MonitoringJob monitoringJob) {
        if (metricsRepository.existsByJobKey(monitoringJob.jobKey())) {
            log.info("Skipping duplicated monitoring job with key={}", monitoringJob.jobKey());
            return;
        }

        log.info("Consuming monitoring job with key={}", monitoringJob.jobKey());

        monitoringConfigurationRepository.findById(monitoringJob.monitoringConfigurationId())
                .ifPresent(configuration -> {
                    if (!configuration.isActive()) {
                        log.info("Skipping inactive configuration job with key={}", monitoringJob.jobKey());
                        return;
                    }
                    if (configuration.getScheduleVersion() != monitoringJob.scheduleVersion()) {
                        log.info("Skipping stale configuration job with key={} expectedVersion={} actualVersion={}",
                                monitoringJob.jobKey(),
                                configuration.getScheduleVersion(),
                                monitoringJob.scheduleVersion());
                        return;
                    }

                    callApiAndStoreMetrics(configuration, monitoringJob.jobKey());
                    scheduleNext(configuration, monitoringJob.runAt());
                });
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
            metricsStreamService.publish(metrics);
        } catch (DataIntegrityViolationException ex) {
            log.info("Duplicate job key ignored: {}", jobKey);
        }
    }

    private void scheduleNext(MonitoringConfiguration monitoringConfiguration, Instant lastRunAt) {
        long intervalMillis = Math.max(MIN_INTERVAL_MILLIS, Math.round(monitoringConfiguration.getInterval() * 1000.0));
        Instant nextRunAt = lastRunAt.plusMillis(intervalMillis);
        Instant now = Instant.now();

        while (!nextRunAt.isAfter(now)) {
            nextRunAt = nextRunAt.plusMillis(intervalMillis);
        }

        monitoringConfiguration.setLastRunAt(LocalDateTime.ofInstant(lastRunAt, ZoneOffset.UTC));
        monitoringConfiguration.setNextRunAt(LocalDateTime.ofInstant(nextRunAt, ZoneOffset.UTC));
        monitoringConfigurationRepository.save(monitoringConfiguration);

        String nextJobKey = toJobKey(
                monitoringConfiguration.getId(),
                monitoringConfiguration.getScheduleVersion(),
                nextRunAt);

        jobPublisher.publish(new MonitoringJob(
                monitoringConfiguration.getId(),
                nextRunAt,
                monitoringConfiguration.getScheduleVersion(),
                nextJobKey));
    }

    private String toJobKey(UUID configurationId, long scheduleVersion, Instant runAt) {
        return configurationId + ":" + scheduleVersion + ":" + runAt.getEpochSecond();
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
