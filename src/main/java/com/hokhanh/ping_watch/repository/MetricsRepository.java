package com.hokhanh.ping_watch.repository;

import java.util.UUID;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hokhanh.ping_watch.model.Metrics;

public interface MetricsRepository extends JpaRepository<Metrics, UUID> {
    boolean existsByJobKey(String jobKey);

    List<Metrics> findByMonitoringConfiguration_IdOrderByTimestampDesc(UUID monitoringConfigurationId, Pageable pageable);
}
