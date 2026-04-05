package com.hokhanh.ping_watch.service.scheduler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.hokhanh.ping_watch.model.Metrics;
import com.hokhanh.ping_watch.response.MonitoringMetricItemResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MetricsStreamService {
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByConfiguration = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID monitoringConfigurationId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByConfiguration
                .computeIfAbsent(monitoringConfigurationId, key -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(monitoringConfigurationId, emitter));
        emitter.onTimeout(() -> removeEmitter(monitoringConfigurationId, emitter));
        emitter.onError(ex -> removeEmitter(monitoringConfigurationId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("metrics-stream-connected"));
        } catch (IOException ex) {
            log.debug("Failed to send initial SSE connected event: {}", ex.getMessage());
            removeEmitter(monitoringConfigurationId, emitter);
        }

        return emitter;
    }

    public void publish(Metrics metrics) {
        UUID monitoringConfigurationId = metrics.getMonitoringConfiguration().getId();
        List<SseEmitter> emitters = emittersByConfiguration.get(monitoringConfigurationId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        MonitoringMetricItemResponse payload = toResponse(metrics);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("metric-created").data(payload));
            } catch (IOException ex) {
                removeEmitter(monitoringConfigurationId, emitter);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        for (Map.Entry<UUID, CopyOnWriteArrayList<SseEmitter>> entry : emittersByConfiguration.entrySet()) {
            UUID monitoringConfigurationId = entry.getKey();
            CopyOnWriteArrayList<SseEmitter> emitters = entry.getValue();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } catch (IOException ex) {
                    removeEmitter(monitoringConfigurationId, emitter);
                }
            }
        }
    }

    private void removeEmitter(UUID monitoringConfigurationId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByConfiguration.get(monitoringConfigurationId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByConfiguration.remove(monitoringConfigurationId);
        }
    }

    private MonitoringMetricItemResponse toResponse(Metrics metrics) {
        return new MonitoringMetricItemResponse(
                metrics.getId(),
                metrics.getStatusCode(),
                metrics.getStatusName(),
                metrics.getResponseTime(),
                metrics.getTimestamp(),
                metrics.isSuccessful(),
                metrics.getJobKey());
    }
}
