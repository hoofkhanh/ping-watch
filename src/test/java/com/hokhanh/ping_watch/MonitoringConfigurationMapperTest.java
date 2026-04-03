package com.hokhanh.ping_watch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.hokhanh.ping_watch.mapper.MonitoringConfigurationMapper;
import com.hokhanh.ping_watch.model.HttpMethod;
import com.hokhanh.ping_watch.model.MonitoringConfiguration;
import com.hokhanh.ping_watch.response.GetAllMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.GetMonitoringConfigurationResponse;

class MonitoringConfigurationMapperTest {
    private final MonitoringConfigurationMapper mapper = new MonitoringConfigurationMapper();

    @Test
    void toGetByIdResponse_shouldMapActiveToStartedStatus() {
        MonitoringConfiguration configuration = MonitoringConfiguration.builder()
                .id(UUID.randomUUID())
                .name("Google Monitor")
                .httpMethod(HttpMethod.GET)
                .url("https://www.google.com")
                .interval(10.0)
                .timeout(5.0)
                .isActive(true)
                .build();

        GetMonitoringConfigurationResponse response = mapper.toGetByIdResponse(configuration);

        assertEquals(true, response.isActive());
        assertEquals("STARTED", response.status());
    }

    @Test
    void toGetAllResponse_shouldMapInactiveToStoppedStatus() {
        MonitoringConfiguration configuration = MonitoringConfiguration.builder()
                .id(UUID.randomUUID())
                .name("Google Monitor")
                .httpMethod(HttpMethod.GET)
                .url("https://www.google.com")
                .interval(10.0)
                .timeout(5.0)
                .isActive(false)
                .build();

        GetAllMonitoringConfigurationResponse response = mapper.toGetAllResponse(
                List.of(configuration),
                0,
                10,
                1,
                1);

        GetAllMonitoringConfigurationResponse.MonitoringConfigurationItemResponse item = response.items().get(0);
        assertEquals(false, item.isActive());
        assertEquals("STOPPED", item.status());
    }
}
