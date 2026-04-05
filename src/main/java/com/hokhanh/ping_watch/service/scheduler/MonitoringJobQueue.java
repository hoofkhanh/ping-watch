package com.hokhanh.ping_watch.service.scheduler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.queue.provider", havingValue = "inmemory", matchIfMissing = true)
public class MonitoringJobQueue implements MonitoringJobPublisher, MonitoringJobConsumer {
    private final DelayQueue<DelayedMonitoringJob> queue = new DelayQueue<>();
    private final Set<String> enqueuedJobKeys = ConcurrentHashMap.newKeySet();

    @Override
    public void publish(MonitoringJob job) {
        if (enqueuedJobKeys.add(job.jobKey())) {
            queue.offer(new DelayedMonitoringJob(job));
        }
    }

    @Override
    public MonitoringJob take() throws InterruptedException {
        DelayedMonitoringJob delayedJob = queue.take();
        enqueuedJobKeys.remove(delayedJob.job().jobKey());
        return delayedJob.job();
    }

    private record DelayedMonitoringJob(MonitoringJob job) implements Delayed {
        @Override
        public long getDelay(TimeUnit unit) {
            long remainingMillis = job.runAt().toEpochMilli() - System.currentTimeMillis();
            return unit.convert(remainingMillis, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) {
                return 0;
            }

            long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
            return Long.compare(diff, 0);
        }
    }
}
