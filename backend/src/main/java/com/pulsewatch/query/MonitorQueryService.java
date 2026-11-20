package com.pulsewatch.query;

import com.pulsewatch.api.CheckResponse;
import com.pulsewatch.api.IncidentResponse;
import com.pulsewatch.api.MonitorResponse;
import com.pulsewatch.api.NotFoundException;
import com.pulsewatch.check.LatencyStats;
import com.pulsewatch.domain.CheckResult;
import com.pulsewatch.domain.CheckResultRepository;
import com.pulsewatch.domain.Incident;
import com.pulsewatch.domain.IncidentRepository;
import com.pulsewatch.domain.Monitor;
import com.pulsewatch.domain.MonitorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Read side: builds the views the dashboard needs. */
@Service
@Transactional(readOnly = true)
public class MonitorQueryService {

    private static final int SPARKLINE_POINTS = 30;
    private static final Duration STATS_WINDOW = Duration.ofHours(24);

    private final MonitorRepository monitors;
    private final CheckResultRepository checks;
    private final IncidentRepository incidents;

    public MonitorQueryService(MonitorRepository monitors, CheckResultRepository checks,
                               IncidentRepository incidents) {
        this.monitors = monitors;
        this.checks = checks;
        this.incidents = incidents;
    }

    public List<MonitorResponse> list() {
        return monitors.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    public MonitorResponse get(long id) {
        return find(id).orElseThrow(() -> NotFoundException.monitor(id));
    }

    public Optional<MonitorResponse> find(long id) {
        return monitors.findById(id).map(this::toResponse);
    }

    public List<CheckResponse> recentChecks(long monitorId, int limit) {
        if (!monitors.existsById(monitorId)) {
            throw NotFoundException.monitor(monitorId);
        }
        int size = Math.max(1, Math.min(limit, 500));
        return checks.findByMonitorIdOrderByCheckedAtDesc(monitorId, PageRequest.of(0, size)).stream()
                .map(CheckResponse::from)
                .toList();
    }

    /** Open incidents only, or the 50 most recent of any state. */
    public List<IncidentResponse> incidents(boolean openOnly) {
        List<Incident> found = openOnly
                ? incidents.findByResolvedAtIsNullOrderByStartedAtDesc()
                : incidents.findAllByOrderByStartedAtDesc(PageRequest.of(0, 50));
        Map<Long, String> names = monitors.findAllById(found.stream().map(Incident::getMonitorId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Monitor::getId, Monitor::getName, (a, b) -> a));
        Instant now = Instant.now();
        return found.stream()
                .map(incident -> toResponse(incident, names.getOrDefault(incident.getMonitorId(), "Deleted monitor"), now))
                .toList();
    }

    public Optional<IncidentResponse> incident(long id) {
        return incidents.findById(id).map(incident -> toResponse(incident,
                monitors.findById(incident.getMonitorId()).map(Monitor::getName).orElse("Deleted monitor"),
                Instant.now()));
    }

    private MonitorResponse toResponse(Monitor monitor) {
        Instant since = Instant.now().minus(STATS_WINDOW);
        List<CheckResult> window = checks.findByMonitorIdAndCheckedAtAfter(monitor.getId(), since).stream()
                .sorted(Comparator.comparing(CheckResult::getCheckedAt))
                .toList();

        List<Long> latencies = window.stream().map(CheckResult::getLatencyMs).toList();
        List<Long> sparkline = latencies.subList(Math.max(0, latencies.size() - SPARKLINE_POINTS), latencies.size());
        Long openIncidentId = incidents.findFirstByMonitorIdAndResolvedAtIsNull(monitor.getId())
                .map(Incident::getId)
                .orElse(null);

        return new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getIntervalSeconds(),
                monitor.getExpectedStatus(),
                monitor.getLatencyThresholdMs(),
                monitor.getFailureThreshold(),
                monitor.isPaused(),
                monitor.isPaused() ? "PAUSED" : monitor.getStatus().name(),
                monitor.getLastCheckedAt(),
                monitor.getLastLatencyMs(),
                LatencyStats.uptimePercent(window.stream().map(CheckResult::getOutcome).toList()),
                LatencyStats.percentile(latencies, 95),
                List.copyOf(sparkline),
                openIncidentId,
                monitor.getCreatedAt());
    }

    private static IncidentResponse toResponse(Incident incident, String monitorName, Instant now) {
        return new IncidentResponse(
                incident.getId(),
                incident.getMonitorId(),
                monitorName,
                incident.getStartedAt(),
                incident.getResolvedAt(),
                incident.getCause(),
                incident.duration(now).toSeconds());
    }
}
