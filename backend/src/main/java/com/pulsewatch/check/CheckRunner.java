package com.pulsewatch.check;

import com.pulsewatch.domain.CheckResult;
import com.pulsewatch.domain.Monitor;
import com.pulsewatch.domain.MonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Runs one check end to end: probe the URL, then record the result. */
@Service
public class CheckRunner {

    private static final Logger log = LoggerFactory.getLogger(CheckRunner.class);

    private final MonitorRepository monitors;
    private final HttpProbe probe;
    private final CheckRecorder recorder;
    private final Duration timeout;

    /** Monitors with a scheduled check in progress, so a slow endpoint is never checked twice at once. */
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public CheckRunner(MonitorRepository monitors, HttpProbe probe, CheckRecorder recorder,
                       @Value("${pulsewatch.probe.timeout-ms:5000}") long timeoutMs) {
        this.monitors = monitors;
        this.probe = probe;
        this.recorder = recorder;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    /** Checks a monitor right now (used by the "Check now" button and by tests). */
    public Optional<CheckResult> runNow(long monitorId) {
        Optional<Monitor> monitor = monitors.findById(monitorId);
        if (monitor.isEmpty()) {
            return Optional.empty();
        }
        ProbeResult result = probe.probe(monitor.get().getUrl(), timeout);
        return recorder.record(monitorId, result, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    /** Scheduled path: skips the monitor if its previous check is still running, and never throws. */
    public void runScheduled(long monitorId) {
        if (!inFlight.add(monitorId)) {
            return;
        }
        try {
            runNow(monitorId);
        } catch (RuntimeException e) {
            log.error("Check for monitor {} failed unexpectedly", monitorId, e);
        } finally {
            inFlight.remove(monitorId);
        }
    }

    public boolean isInFlight(long monitorId) {
        return inFlight.contains(monitorId);
    }
}
