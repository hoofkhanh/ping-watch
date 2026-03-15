package com.hokhanh.ping_watch.model;

import java.time.LocalDateTime;
import java.util.UUID;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "metrics")
public class Metrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private int statusCode;
    private String statusName;
    private double responseTime; // in seconds
    private LocalDateTime timestamp;
    private boolean isSuccessful;

    @ManyToOne
    @JoinColumn(name = "monitoring_configuration_id", nullable = false)
    private MonitoringConfiguration monitoringConfiguration;
}
