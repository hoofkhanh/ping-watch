package com.hokhanh.ping_watch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.hokhanh.ping_watch.service.scheduler.MonitoringJob;
import com.hokhanh.ping_watch.service.scheduler.MonitoringJobQueue;

class MonitoringJobQueueTest {

    @Test
    void publishAndPoll_shouldWorkInMemory() {
        MonitoringJobQueue queue = new MonitoringJobQueue();
        MonitoringJob job = new MonitoringJob(UUID.randomUUID(), Instant.now(), "job-key");

        queue.publish(job);
        MonitoringJob result = queue.poll();

        assertNotNull(result);
        assertEquals(job.monitoringConfigurationId(), result.monitoringConfigurationId());
        assertEquals(job.jobKey(), result.jobKey());
    }
}
