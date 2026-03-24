package com.hokhanh.ping_watch.request;

import jakarta.validation.constraints.Min;

public record GetAllMonitoringConfigurationRequest(
        @Min(value = 0, message = "Page must be greater than or equal to 0")
        Integer page,

        @Min(value = 1, message = "Size must be greater than 0")
        Integer size) {

}
