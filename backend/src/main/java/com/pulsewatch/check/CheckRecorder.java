package com.pulsewatch.check;

import com.pulsewatch.domain.CheckOutcome;
import com.pulsewatch.domain.CheckResult;
import com.pulsewatch.domain.CheckResultRepository;
import com.pulsewatch.domain.Incident;
import com.pulsewatch.domain.IncidentRepository;
import com.pulsewatch.domain.Monitor;
import com.pulsewatch.domain.MonitorRepository;
import com.pulsewatch.live.IncidentChangedEvent;
import com.pulsewatch.live.MonitorChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Saves a probe result and applies the state machine, all in one transaction.
 *
 * <p>The HTTP call itself happens before this, outside any transaction, so a slow endpoint
 * never holds a database connection open.
 */
@Service
public class CheckRecorder {

    private static final int MAX_ERROR_LENGTH = 500;

    private final MonitorRepository monitors;
    private final CheckResultRepository checks;
    private final IncidentRepository incidents;
    private final ApplicationEventPublisher events;

    public CheckRecorder(MonitorRepository monitors, CheckResultRepository checks,
                         IncidentRepository incidents, ApplicationEventPublisher events) {
        this.monitors = monitors;
        this.checks = checks;
        this.incidents = incidents;
        this.events = events;
    }

    /** Returns empty if the monitor was deleted while its check was in flight. */
    @Transactional
    public Optional<CheckResult> record(long monitorId, ProbeResult probe, Instant checkedAt) {
        Optional<Monitor> found = monitors.findById(monitorId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Monitor monitor = found.get();

        CheckOutcome outcome = OutcomeClassifier.classify(
                probe, monitor.getExpectedStatus(), monitor.getLatencyThresholdMs());
        CheckResult result = checks.save(new CheckResult(
                monitorId, checkedAt, outcome, probe.statusCode(), probe.latencyMs(), truncate(probe.error())));

        StatusEvaluator.Transition transition = StatusEvaluator.evaluate(
                monitor.getStatus(), monitor.getConsecutiveFailures(), monitor.getFailureThreshold(), outcome);
        monitor.recordCheck(transition.status(), transition.consecutiveFailures(), checkedAt, probe.latencyMs());

        if (transition.openIncident()) {
            Incident incident = incidents.save(
                    new Incident(monitorId, firstFailureTime(monitorId, checkedAt), describeFailure(probe, monitor)));
            events.publishEvent(new IncidentChangedEvent(incident.getId()));
        }
        if (transition.resolveIncident()) {
            incidents.findFirstByMonitorIdAndResolvedAtIsNull(monitorId).ifPresent(incident -> {
                incident.resolve(checkedAt);
                events.publishEvent(new IncidentChangedEvent(incident.getId()));
            });
        }

        events.publishEvent(new MonitorChangedEvent(monitorId));
        return Optional.of(result);
    }

    /**
     * An incident is only opened after several failures, but the outage began at the first one.
     * Walk back through the latest checks to find when the current run of failures started.
     */
    private Instant firstFailureTime(long monitorId, Instant fallback) {
        List<CheckResult> recent = checks.findByMonitorIdOrderByCheckedAtDesc(monitorId, PageRequest.of(0, 50));
        Instant start = fallback;
        for (CheckResult check : recent) {
            if (check.getOutcome() != CheckOutcome.DOWN) {
                break;
            }
            start = check.getCheckedAt();
        }
        return start;
    }

    private static String describeFailure(ProbeResult probe, Monitor monitor) {
        String cause = probe.error() != null
                ? probe.error()
                : "HTTP " + probe.statusCode() + " (expected " + monitor.getExpectedStatus() + ")";
        return truncate(cause);
    }

    private static String truncate(String text) {
        if (text == null || text.length() <= MAX_ERROR_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_ERROR_LENGTH - 3) + "...";
    }
}
