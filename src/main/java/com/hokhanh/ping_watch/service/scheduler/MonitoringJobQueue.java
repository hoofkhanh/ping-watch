package com.hokhanh.ping_watch.service.scheduler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Service;

@Service
public class MonitoringJobQueue {
    private final BlockingQueue<MonitoringJob> queue = new LinkedBlockingQueue<>();

    public void enqueue(MonitoringJob job) {
        queue.offer(job);
    }

    public MonitoringJob poll() {
        return queue.poll();
    }
}
