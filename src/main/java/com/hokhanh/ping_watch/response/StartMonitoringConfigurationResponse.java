package com.hokhanh.ping_watch.response;

import java.util.UUID;

public record StartMonitoringConfigurationResponse(
        UUID id,
        String status,
        String message) {

}
