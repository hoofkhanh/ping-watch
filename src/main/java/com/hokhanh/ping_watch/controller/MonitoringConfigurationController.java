package com.hokhanh.ping_watch.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hokhanh.ping_watch.request.AddMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.request.DeleteMonitoringConfigurationRequest;
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
import com.hokhanh.ping_watch.service.MonitoringConfigurationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@ConditionalOnExpression("'${app.role:all}' == 'api' || '${app.role:all}' == 'all'")
@RequestMapping("/monitoring-configurations")
@Slf4j
@Validated
@RequiredArgsConstructor
public class MonitoringConfigurationController {
    private final MonitoringConfigurationService monitoringConfigurationService;

    @PostMapping
    public ResponseEntity<AddMonitoringConfigurationResponse> add(
            @RequestBody @Valid AddMonitoringConfigurationRequest request,
            @AuthenticationPrincipal String userId) {
        log.info("Received add monitoring configuration request: {}", request);

        AddMonitoringConfigurationResponse response = monitoringConfigurationService.add(request, userId);
        URI location = URI.create("/monitoring-configurations/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UpdateMonitoringConfigurationResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateMonitoringConfigurationRequest request,
            @AuthenticationPrincipal String userId) {
        log.info("Received update monitoring configuration request with id: {}", id);

        UpdateMonitoringConfigurationResponse response = monitoringConfigurationService.update(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<DeleteMonitoringConfigurationResponse> delete(
            @RequestBody @Valid DeleteMonitoringConfigurationRequest request,
            @AuthenticationPrincipal String userId) {
        log.info("Received delete monitoring configuration request with id: {}", request.id());

        DeleteMonitoringConfigurationResponse response = monitoringConfigurationService.delete(request.id(), userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<GetAllMonitoringConfigurationResponse> getAll(
            @ModelAttribute @Valid GetAllMonitoringConfigurationRequest request,
            @AuthenticationPrincipal String userId) {
        log.info("Received get all monitoring configurations request: {}", request);

        GetAllMonitoringConfigurationResponse response = monitoringConfigurationService.getAll(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetMonitoringConfigurationResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        log.info("Received get monitoring configuration by id request with id: {}", id);

        GetMonitoringConfigurationResponse response = monitoringConfigurationService.getById(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<GetMonitoringMetricsResponse> getMetrics(
            @PathVariable UUID id,
            @RequestParam(required = false) @Min(value = 1, message = "Limit must be greater than 0") Integer limit,
            @AuthenticationPrincipal String userId) {
        log.info("Received get monitoring metrics request with id: {}, limit: {}", id, limit);

        GetMonitoringMetricsResponse response = monitoringConfigurationService.getMetrics(id, limit, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{id}/metrics/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        log.info("Received stream monitoring metrics request with id: {}", id);

        return monitoringConfigurationService.streamMetrics(id, userId);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<StartMonitoringConfigurationResponse> start(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        log.info("Received start monitoring request with id: {}", id);

        StartMonitoringConfigurationResponse response = monitoringConfigurationService.start(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<StopMonitoringConfigurationResponse> stop(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        log.info("Received stop monitoring request with id: {}", id);

        StopMonitoringConfigurationResponse response = monitoringConfigurationService.stop(id, userId);
        return ResponseEntity.ok(response);
    }
}
