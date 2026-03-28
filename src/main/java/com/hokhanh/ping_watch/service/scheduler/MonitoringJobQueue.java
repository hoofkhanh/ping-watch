package com.hokhanh.ping_watch.service.scheduler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.queue.provider", havingValue = "inmemory", matchIfMissing = true)
public class MonitoringJobQueue implements MonitoringJobPublisher, MonitoringJobConsumer {
    private final BlockingQueue<MonitoringJob> queue = new LinkedBlockingQueue<>();

    @Override
    public void publish(MonitoringJob job) {
        queue.offer(job);
    }

    @Override
    public MonitoringJob poll() {
        return queue.poll();
    }
}
