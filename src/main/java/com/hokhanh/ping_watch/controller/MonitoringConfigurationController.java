package com.hokhanh.ping_watch.controller;

import java.net.URI;
import java.util.UUID;

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
import com.hokhanh.ping_watch.response.GetMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.UpdateMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.service.MonitoringConfigurationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
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
}
