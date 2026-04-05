package com.hokhanh.ping_watch.service;

import java.util.UUID;

import com.hokhanh.ping_watch.request.AddMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.request.GetAllMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.request.UpdateMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.response.AddMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.DeleteMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.GetAllMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.GetMonitoringMetricsResponse;
import com.hokhanh.ping_watch.response.GetMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.StartMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.StopMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.UpdateMonitoringConfigurationResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface MonitoringConfigurationService {
    AddMonitoringConfigurationResponse add(AddMonitoringConfigurationRequest request, String userId);

    UpdateMonitoringConfigurationResponse update(UUID id, UpdateMonitoringConfigurationRequest request, String userId);

    DeleteMonitoringConfigurationResponse delete(UUID id, String userId);

    GetAllMonitoringConfigurationResponse getAll(GetAllMonitoringConfigurationRequest request, String userId);

    GetMonitoringConfigurationResponse getById(UUID id, String userId);

    GetMonitoringMetricsResponse getMetrics(UUID id, Integer limit, String userId);

    SseEmitter streamMetrics(UUID id, String userId);

    StartMonitoringConfigurationResponse start(UUID id, String userId);

    StopMonitoringConfigurationResponse stop(UUID id, String userId);
}
