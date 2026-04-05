package com.hokhanh.ping_watch.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.hokhanh.ping_watch.constant.ErrorCode;
import com.hokhanh.ping_watch.mapper.MonitoringConfigurationMapper;
import com.hokhanh.ping_watch.model.Metrics;
import com.hokhanh.ping_watch.model.MonitoringConfiguration;
import com.hokhanh.ping_watch.model.User;
import com.hokhanh.ping_watch.repository.MetricsRepository;
import com.hokhanh.ping_watch.repository.MonitoringConfigurationRepository;
import com.hokhanh.ping_watch.repository.UserRepository;
import com.hokhanh.ping_watch.request.AddMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.request.GetAllMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.request.UpdateMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.response.AddMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.DeleteMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.GetAllMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.GetMonitoringMetricsResponse;
import com.hokhanh.ping_watch.response.GetMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.MonitoringMetricItemResponse;
import com.hokhanh.ping_watch.response.StartMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.StopMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.UpdateMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.service.MonitoringConfigurationService;
import com.hokhanh.ping_watch.service.scheduler.MetricsStreamService;
import com.hokhanh.ping_watch.service.scheduler.MonitoringJob;
import com.hokhanh.ping_watch.service.scheduler.MonitoringJobPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringConfigurationServiceImpl implements MonitoringConfigurationService {
    private static final int DEFAULT_METRICS_LIMIT = 50;
    private static final int MAX_METRICS_LIMIT = 200;

    private final MonitoringConfigurationRepository monitoringConfigurationRepository;
    private final UserRepository userRepository;
    private final MetricsRepository metricsRepository;
    private final MonitoringConfigurationMapper monitoringConfigurationMapper;
    private final MetricsStreamService metricsStreamService;
    private final MonitoringJobPublisher monitoringJobPublisher;

    @Override
    public AddMonitoringConfigurationResponse add(AddMonitoringConfigurationRequest request, String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing add monitoring configuration request for user: {}", parsedUserId);

        User user = userRepository.findById(parsedUserId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.USER_NOT_FOUND.name()));

        MonitoringConfiguration monitoringConfiguration = monitoringConfigurationMapper.toEntity(request, user);
        MonitoringConfiguration savedMonitoringConfiguration = monitoringConfigurationRepository
                .save(monitoringConfiguration);
        return monitoringConfigurationMapper.toAddResponse(savedMonitoringConfiguration);
    }

    @Override
    public UpdateMonitoringConfigurationResponse update(UUID id, UpdateMonitoringConfigurationRequest request,
            String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing update monitoring configuration request with id: {}, userId: {}", id, parsedUserId);

        MonitoringConfiguration monitoringConfiguration = getMonitoringConfigurationByIdAndUserId(id, parsedUserId);

        monitoringConfigurationMapper.updateEntity(monitoringConfiguration, request);
        monitoringConfiguration.setScheduleVersion(monitoringConfiguration.getScheduleVersion() + 1);
        if (monitoringConfiguration.isActive()) {
            LocalDateTime now = LocalDateTime.now();
            monitoringConfiguration.setNextRunAt(nextRunAtFromInterval(now, monitoringConfiguration.getInterval()));
        }
        MonitoringConfiguration updatedMonitoringConfiguration = monitoringConfigurationRepository
                .save(monitoringConfiguration);
        return monitoringConfigurationMapper.toUpdateResponse(updatedMonitoringConfiguration);
    }

    @Override
    public DeleteMonitoringConfigurationResponse delete(UUID id, String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing delete monitoring configuration request with id: {}, userId: {}", id, parsedUserId);

        MonitoringConfiguration monitoringConfiguration = getMonitoringConfigurationByIdAndUserId(id, parsedUserId);

        monitoringConfigurationRepository.delete(monitoringConfiguration);
        return new DeleteMonitoringConfigurationResponse(id, "Monitoring configuration deleted successfully");
    }

    @Override
    public GetAllMonitoringConfigurationResponse getAll(GetAllMonitoringConfigurationRequest request, String userId) {
        UUID parsedUserId = parseUserId(userId);
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 10 : request.size();

        log.info("Processing get all monitoring configurations request with page: {}, size: {}, userId: {}", page, size,
                parsedUserId);

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<MonitoringConfiguration> monitoringConfigurationPage = monitoringConfigurationRepository.findAllByUser_Id(
                parsedUserId, pageRequest);

        return monitoringConfigurationMapper.toGetAllResponse(
                monitoringConfigurationPage.getContent(),
                monitoringConfigurationPage.getNumber(),
                monitoringConfigurationPage.getSize(),
                monitoringConfigurationPage.getTotalPages(),
                monitoringConfigurationPage.getTotalElements());
    }

    @Override
    public GetMonitoringConfigurationResponse getById(UUID id, String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing get monitoring configuration by id request with id: {}, userId: {}", id, parsedUserId);

        MonitoringConfiguration monitoringConfiguration = getMonitoringConfigurationByIdAndUserId(id, parsedUserId);
        return monitoringConfigurationMapper.toGetByIdResponse(monitoringConfiguration);
    }

    @Override
    public GetMonitoringMetricsResponse getMetrics(UUID id, Integer limit, String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing get monitoring metrics request with id: {}, userId: {}", id, parsedUserId);

        MonitoringConfiguration monitoringConfiguration = getMonitoringConfigurationByIdAndUserId(id, parsedUserId);

        int safeLimit = normalizeLimit(limit);
        List<Metrics> metrics = metricsRepository.findByMonitoringConfiguration_IdOrderByTimestampDesc(
                monitoringConfiguration.getId(),
                PageRequest.of(0, safeLimit));

        List<MonitoringMetricItemResponse> items = metrics.stream()
                .map(monitoringConfigurationMapper::toMetricItemResponse)
                .toList();

        long totalChecks = items.size();
        long successCount = items.stream().filter(MonitoringMetricItemResponse::isSuccessful).count();
        long failureCount = totalChecks - successCount;
        double successRate = totalChecks == 0 ? 0 : (successCount * 100.0) / totalChecks;
        double avgResponseTime = totalChecks == 0 ? 0
                : items.stream().mapToDouble(MonitoringMetricItemResponse::responseTime).average().orElse(0);
        String latestStatus = items.isEmpty() ? null : items.get(0).statusName();
        LocalDateTime latestTimestamp = items.isEmpty() ? null : items.get(0).timestamp();

        return new GetMonitoringMetricsResponse(
                items,
                new GetMonitoringMetricsResponse.MetricsSummary(
                        totalChecks,
                        successCount,
                        failureCount,
                        successRate,
                        avgResponseTime,
                        latestStatus,
                        latestTimestamp));
    }

    @Override
    public SseEmitter streamMetrics(UUID id, String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing metrics stream request with id: {}, userId: {}", id, parsedUserId);

        MonitoringConfiguration monitoringConfiguration = getMonitoringConfigurationByIdAndUserId(id, parsedUserId);
        return metricsStreamService.subscribe(monitoringConfiguration.getId());
    }

    @Override
    public StartMonitoringConfigurationResponse start(UUID id, String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing start monitoring request with id: {}, userId: {}", id, parsedUserId);

        MonitoringConfiguration monitoringConfiguration = getMonitoringConfigurationByIdAndUserId(id, parsedUserId);
        if (monitoringConfiguration.isActive()) {
            throw new IllegalArgumentException(ErrorCode.MONITORING_CONFIGURATION_ALREADY_STARTED.name());
        }

        Instant nowInstant = Instant.now();
        LocalDateTime now = LocalDateTime.ofInstant(nowInstant, ZoneOffset.UTC);
        long nextScheduleVersion = monitoringConfiguration.getScheduleVersion() + 1;
        monitoringConfiguration.setActive(true);
        monitoringConfiguration.setLastRunAt(null);
        monitoringConfiguration.setScheduleVersion(nextScheduleVersion);
        monitoringConfiguration.setNextRunAt(nextRunAtFromInterval(now, monitoringConfiguration.getInterval()));
        monitoringConfigurationRepository.save(monitoringConfiguration);
        monitoringJobPublisher.publish(new MonitoringJob(
                monitoringConfiguration.getId(),
                nowInstant,
                nextScheduleVersion,
                toJobKey(monitoringConfiguration.getId(), nextScheduleVersion, nowInstant)));

        return new StartMonitoringConfigurationResponse(
                monitoringConfiguration.getId(),
                "STARTED",
                "Monitoring scheduler started successfully");
    }

    @Override
    public StopMonitoringConfigurationResponse stop(UUID id, String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing stop monitoring request with id: {}, userId: {}", id, parsedUserId);

        MonitoringConfiguration monitoringConfiguration = getMonitoringConfigurationByIdAndUserId(id, parsedUserId);
        if (!monitoringConfiguration.isActive()) {
            throw new IllegalArgumentException(ErrorCode.MONITORING_CONFIGURATION_ALREADY_STOPPED.name());
        }

        monitoringConfiguration.setScheduleVersion(monitoringConfiguration.getScheduleVersion() + 1);
        monitoringConfiguration.setActive(false);
        monitoringConfiguration.setNextRunAt(null);
        monitoringConfigurationRepository.save(monitoringConfiguration);

        return new StopMonitoringConfigurationResponse(
                monitoringConfiguration.getId(),
                "STOPPED",
                "Monitoring scheduler stopped successfully");
    }

    private MonitoringConfiguration getMonitoringConfigurationByIdAndUserId(UUID id, UUID userId) {
        return monitoringConfigurationRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MONITORING_CONFIGURATION_NOT_FOUND.name()));
    }

    private UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ErrorCode.INVALID_USER_ID.name());
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_METRICS_LIMIT;
        }

        return Math.min(limit, MAX_METRICS_LIMIT);
    }

    private String toJobKey(UUID configurationId, long scheduleVersion, Instant runAt) {
        return configurationId + ":" + scheduleVersion + ":" + runAt.getEpochSecond();
    }

    private LocalDateTime nextRunAtFromInterval(LocalDateTime now, double intervalSeconds) {
        long intervalMillis = Math.max(3000L, Math.round(intervalSeconds * 1000.0));
        return now.plusNanos(intervalMillis * 1_000_000L);
    }

}
