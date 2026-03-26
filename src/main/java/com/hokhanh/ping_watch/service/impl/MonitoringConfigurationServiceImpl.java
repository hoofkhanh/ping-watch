package com.hokhanh.ping_watch.service.impl;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.hokhanh.ping_watch.constant.ErrorCode;
import com.hokhanh.ping_watch.mapper.MonitoringConfigurationMapper;
import com.hokhanh.ping_watch.model.MonitoringConfiguration;
import com.hokhanh.ping_watch.model.User;
import com.hokhanh.ping_watch.repository.MonitoringConfigurationRepository;
import com.hokhanh.ping_watch.repository.UserRepository;
import com.hokhanh.ping_watch.request.AddMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.request.GetAllMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.request.UpdateMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.response.AddMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.DeleteMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.GetAllMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.GetMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.StartMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.StopMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.UpdateMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.service.MonitoringConfigurationService;
import com.hokhanh.ping_watch.service.scheduler.MonitoringRunStateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringConfigurationServiceImpl implements MonitoringConfigurationService {
    private final MonitoringConfigurationRepository monitoringConfigurationRepository;
    private final UserRepository userRepository;
    private final MonitoringConfigurationMapper monitoringConfigurationMapper;
    private final MonitoringRunStateService monitoringRunStateService;

    @Override
    public AddMonitoringConfigurationResponse add(AddMonitoringConfigurationRequest request, String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing add monitoring configuration request for user: {}", parsedUserId);

        User user = userRepository.findById(parsedUserId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.USER_NOT_FOUND.name()));

        MonitoringConfiguration monitoringConfiguration = monitoringConfigurationMapper.toEntity(request, user);
        MonitoringConfiguration savedMonitoringConfiguration = monitoringConfigurationRepository.save(monitoringConfiguration);
        return monitoringConfigurationMapper.toAddResponse(savedMonitoringConfiguration);
    }

    @Override
    public UpdateMonitoringConfigurationResponse update(UUID id, UpdateMonitoringConfigurationRequest request, String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing update monitoring configuration request with id: {}, userId: {}", id, parsedUserId);

        MonitoringConfiguration monitoringConfiguration = getMonitoringConfigurationByIdAndUserId(id, parsedUserId);

        monitoringConfigurationMapper.updateEntity(monitoringConfiguration, request);
        MonitoringConfiguration updatedMonitoringConfiguration = monitoringConfigurationRepository.save(monitoringConfiguration);
        return monitoringConfigurationMapper.toUpdateResponse(updatedMonitoringConfiguration);
    }

    @Override
    public DeleteMonitoringConfigurationResponse delete(UUID id, String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing delete monitoring configuration request with id: {}, userId: {}", id, parsedUserId);

        MonitoringConfiguration monitoringConfiguration = getMonitoringConfigurationByIdAndUserId(id, parsedUserId);

        monitoringConfigurationRepository.delete(monitoringConfiguration);
        monitoringRunStateService.stop(id);
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
    public StartMonitoringConfigurationResponse start(UUID id, String userId) {
        UUID parsedUserId = parseUserId(userId);
        log.info("Processing start monitoring request with id: {}, userId: {}", id, parsedUserId);

        MonitoringConfiguration monitoringConfiguration = getMonitoringConfigurationByIdAndUserId(id, parsedUserId);
        monitoringRunStateService.start(monitoringConfiguration.getId());

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
        monitoringRunStateService.stop(monitoringConfiguration.getId());

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
}
