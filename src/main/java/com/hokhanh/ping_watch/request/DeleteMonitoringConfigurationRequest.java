package com.hokhanh.ping_watch.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record DeleteMonitoringConfigurationRequest(
        @NotNull(message = "Monitoring configuration id is required")
        UUID id) {

}
