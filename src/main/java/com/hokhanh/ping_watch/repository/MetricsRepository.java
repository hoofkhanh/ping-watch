package com.hokhanh.ping_watch.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hokhanh.ping_watch.model.Metrics;

public interface MetricsRepository extends JpaRepository<Metrics, UUID> {

}
