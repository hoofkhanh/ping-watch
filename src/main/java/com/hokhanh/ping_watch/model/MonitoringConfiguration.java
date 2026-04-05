package com.hokhanh.ping_watch.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.Builder.Default;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "monitoring_configuration")
public class MonitoringConfiguration {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String name;

  @Enumerated(EnumType.STRING)
  private HttpMethod httpMethod;
  private String url;
  private double interval; // in seconds
  private double timeout; // in seconds

  @Default
  @Column(name = "is_active")
  private boolean isActive = false;

  private LocalDateTime nextRunAt;
  private LocalDateTime lastRunAt;

  @Default
  @Column(name = "schedule_version", nullable = false)
  private long scheduleVersion = 0L;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

}
