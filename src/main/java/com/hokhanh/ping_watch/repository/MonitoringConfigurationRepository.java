package com.hokhanh.ping_watch.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hokhanh.ping_watch.model.MonitoringConfiguration;

public interface MonitoringConfigurationRepository extends JpaRepository<MonitoringConfiguration, UUID> {
    Page<MonitoringConfiguration> findAllByUser_Id(UUID userId, Pageable pageable);

    Optional<MonitoringConfiguration> findByIdAndUser_Id(UUID id, UUID userId);
}
