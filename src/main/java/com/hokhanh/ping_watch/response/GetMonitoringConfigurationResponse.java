package com.hokhanh.ping_watch.response;

import java.util.UUID;

import com.hokhanh.ping_watch.model.HttpMethod;

public record GetMonitoringConfigurationResponse(
        UUID id,
        String name,
        HttpMethod httpMethod,
        String url,
        double interval,
        double timeout) {

}
