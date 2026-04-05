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
    void publishAndTake_shouldWorkInMemory() throws InterruptedException {
        MonitoringJobQueue queue = new MonitoringJobQueue();
        MonitoringJob job = new MonitoringJob(UUID.randomUUID(), Instant.now().minusMillis(1), 1L, "job-key");

        queue.publish(job);
        MonitoringJob result = queue.take();

        assertNotNull(result);
        assertEquals(job.monitoringConfigurationId(), result.monitoringConfigurationId());
        assertEquals(job.scheduleVersion(), result.scheduleVersion());
        assertEquals(job.jobKey(), result.jobKey());
    }
}
