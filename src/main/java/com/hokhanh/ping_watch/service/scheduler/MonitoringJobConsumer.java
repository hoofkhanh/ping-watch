package com.hokhanh.ping_watch.service.scheduler;

public interface MonitoringJobConsumer {
    MonitoringJob take() throws InterruptedException;
}
