package com.hokhanh.ping_watch.response;

import java.util.UUID;

public record StopMonitoringConfigurationResponse(
        UUID id,
        String status,
        String message) {

}
