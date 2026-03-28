package com.hokhanh.ping_watch.service.scheduler;

public interface MonitoringJobPublisher {
    void publish(MonitoringJob job);
}
