package com.hokhanh.ping_watch.response;

import java.util.List;
import java.util.UUID;

import com.hokhanh.ping_watch.model.HttpMethod;

public record GetAllMonitoringConfigurationResponse(
        List<MonitoringConfigurationItemResponse> items,
        int page,
        int size,
        int totalPages,
        long totalElements) {

    public record MonitoringConfigurationItemResponse(
            UUID id,
            String name,
            HttpMethod httpMethod,
            String url,
            double interval,
            double timeout) {

    }
}
