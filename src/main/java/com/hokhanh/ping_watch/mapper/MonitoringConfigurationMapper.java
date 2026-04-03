package com.hokhanh.ping_watch.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.hokhanh.ping_watch.model.MonitoringConfiguration;
import com.hokhanh.ping_watch.model.User;
import com.hokhanh.ping_watch.request.AddMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.request.UpdateMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.response.AddMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.GetAllMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.GetAllMonitoringConfigurationResponse.MonitoringConfigurationItemResponse;
import com.hokhanh.ping_watch.response.GetMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.UpdateMonitoringConfigurationResponse;

@Component
public class MonitoringConfigurationMapper {
    private static final String STATUS_STARTED = "STARTED";
    private static final String STATUS_STOPPED = "STOPPED";

    public MonitoringConfiguration toEntity(AddMonitoringConfigurationRequest request, User user) {
        return MonitoringConfiguration.builder()
                .name(request.name())
                .httpMethod(request.httpMethod())
                .url(request.url())
                .interval(request.interval())
                .timeout(request.timeout())
                .isActive(false)
                .nextRunAt(null)
                .lastRunAt(null)
                .user(user)
                .build();
    }

    public void updateEntity(MonitoringConfiguration monitoringConfiguration, UpdateMonitoringConfigurationRequest request) {
        monitoringConfiguration.setName(request.name());
        monitoringConfiguration.setHttpMethod(request.httpMethod());
        monitoringConfiguration.setUrl(request.url());
        monitoringConfiguration.setInterval(request.interval());
        monitoringConfiguration.setTimeout(request.timeout());
    }

    public AddMonitoringConfigurationResponse toAddResponse(MonitoringConfiguration monitoringConfiguration) {
        return new AddMonitoringConfigurationResponse(
                monitoringConfiguration.getId(),
                monitoringConfiguration.getName(),
                monitoringConfiguration.getHttpMethod(),
                monitoringConfiguration.getUrl(),
                monitoringConfiguration.getInterval(),
                monitoringConfiguration.getTimeout());
    }

    public UpdateMonitoringConfigurationResponse toUpdateResponse(MonitoringConfiguration monitoringConfiguration) {
        return new UpdateMonitoringConfigurationResponse(
                monitoringConfiguration.getId(),
                monitoringConfiguration.getName(),
                monitoringConfiguration.getHttpMethod(),
                monitoringConfiguration.getUrl(),
                monitoringConfiguration.getInterval(),
                monitoringConfiguration.getTimeout());
    }

    public GetAllMonitoringConfigurationResponse toGetAllResponse(List<MonitoringConfiguration> monitoringConfigurations,
            int page,
            int size,
            int totalPages,
            long totalElements) {

        List<MonitoringConfigurationItemResponse> items = monitoringConfigurations.stream()
                .map(this::toItemResponse)
                .toList();

        return new GetAllMonitoringConfigurationResponse(items, page, size, totalPages, totalElements);
    }

    public GetMonitoringConfigurationResponse toGetByIdResponse(MonitoringConfiguration monitoringConfiguration) {
        return new GetMonitoringConfigurationResponse(
                monitoringConfiguration.getId(),
                monitoringConfiguration.getName(),
                monitoringConfiguration.getHttpMethod(),
                monitoringConfiguration.getUrl(),
                monitoringConfiguration.getInterval(),
                monitoringConfiguration.getTimeout(),
                monitoringConfiguration.isActive(),
                toStatus(monitoringConfiguration.isActive()));
    }

    private MonitoringConfigurationItemResponse toItemResponse(MonitoringConfiguration monitoringConfiguration) {
        return new MonitoringConfigurationItemResponse(
                monitoringConfiguration.getId(),
                monitoringConfiguration.getName(),
                monitoringConfiguration.getHttpMethod(),
                monitoringConfiguration.getUrl(),
                monitoringConfiguration.getInterval(),
                monitoringConfiguration.getTimeout(),
                monitoringConfiguration.isActive(),
                toStatus(monitoringConfiguration.isActive()));
    }

    private String toStatus(boolean isActive) {
        return isActive ? STATUS_STARTED : STATUS_STOPPED;
    }
}
