package com.pulsewatch.check;

import com.pulsewatch.domain.CheckResultRepository;
import com.pulsewatch.domain.Monitor;
import com.pulsewatch.domain.MonitorRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Every tick, finds monitors whose interval has passed and checks them on a worker pool.
 * One slow endpoint therefore never delays the others. Disabled in tests.
 */
@Component
@ConditionalOnProperty(name = "pulsewatch.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class CheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(CheckScheduler.class);

    private final MonitorRepository monitors;
    private final CheckResultRepository checks;
    private final CheckRunner runner;
    private final ExecutorService workers;
    private final int retentionDays;

    public CheckScheduler(MonitorRepository monitors, CheckResultRepository checks, CheckRunner runner,
                          @Value("${pulsewatch.scheduler.threads:8}") int threads,
                          @Value("${pulsewatch.retention-days:7}") int retentionDays) {
        this.monitors = monitors;
        this.checks = checks;
        this.runner = runner;
        this.workers = Executors.newFixedThreadPool(threads);
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "${pulsewatch.scheduler.tick-ms:1000}")
    public void dispatchDueChecks() {
        Instant now = Instant.now();
        for (Monitor monitor : monitors.findByPausedFalse()) {
            if (monitor.isDue(now) && !runner.isInFlight(monitor.getId())) {
                long id = monitor.getId();
                workers.submit(() -> runner.runScheduled(id));
            }
        }
    }

    /** Keeps the check history from growing forever. Runs daily at 03:00. */
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldChecks() {
        long deleted = checks.deleteByCheckedAtBefore(Instant.now().minus(Duration.ofDays(retentionDays)));
        log.info("Retention: deleted {} check results older than {} days", deleted, retentionDays);
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        workers.shutdown();
        if (!workers.awaitTermination(10, TimeUnit.SECONDS)) {
            workers.shutdownNow();
        }
    }
}
