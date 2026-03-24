package com.hokhanh.ping_watch.request;

import com.hokhanh.ping_watch.model.HttpMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMonitoringConfigurationRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Http method is required")
        HttpMethod httpMethod,

        @NotBlank(message = "URL is required")
        String url,

        @DecimalMin(value = "0.1", message = "Interval must be greater than 0")
        double interval,

        @DecimalMin(value = "0.1", message = "Timeout must be greater than 0")
        double timeout) {

}
