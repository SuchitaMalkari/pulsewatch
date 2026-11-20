package com.pulsewatch.check;

import com.pulsewatch.api.MonitorRequest;
import com.pulsewatch.command.MonitorCommandService;
import com.pulsewatch.domain.CheckOutcome;
import com.pulsewatch.domain.CheckResult;
import com.pulsewatch.domain.CheckResultRepository;
import com.pulsewatch.domain.Incident;
import com.pulsewatch.domain.IncidentRepository;
import com.pulsewatch.domain.Monitor;
import com.pulsewatch.domain.MonitorRepository;
import com.pulsewatch.domain.MonitorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check pipeline with a fake HTTP probe: probe -> classify -> state machine ->
 * database. Proves incidents open after N failures, start at the first failure, and resolve.
 */
@SpringBootTest
@Import(CheckFlowTest.FakeProbeConfig.class)
class CheckFlowTest {

    @TestConfiguration
    static class FakeProbeConfig {
        @Bean
        @Primary
        FakeProbe fakeProbe() {
            return new FakeProbe();
        }
    }

    /** Returns queued results in order, then healthy responses. */
    static class FakeProbe implements HttpProbe {
        private final Deque<ProbeResult> queue = new ConcurrentLinkedDeque<>();

        void willReturn(ProbeResult... results) {
            queue.addAll(List.of(results));
        }

        @Override
        public ProbeResult probe(String url, Duration timeout) {
            ProbeResult next = queue.poll();
            return next != null ? next : new ProbeResult(200, 25, null);
        }
    }

    @Autowired private FakeProbe probe;
    @Autowired private CheckRunner runner;
    @Autowired private MonitorCommandService commands;
    @Autowired private MonitorRepository monitors;
    @Autowired private CheckResultRepository checks;
    @Autowired private IncidentRepository incidents;

    private long monitorId;

    @BeforeEach
    void setUp() {
        monitors.deleteAll();
        monitorId = commands.create(new MonitorRequest("Inventory", "https://inventory.test/health", 30, 200, 500, 3));
    }

    @Test
    void two_failures_are_tolerated_and_the_third_opens_an_incident() throws InterruptedException {
        probe.willReturn(down(), down(), down());

        runner.runNow(monitorId);
        Thread.sleep(5);
        runner.runNow(monitorId);
        assertThat(monitor().getStatus()).isEqualTo(MonitorStatus.PENDING);
        assertThat(openIncident()).isNull();

        runner.runNow(monitorId);
        assertThat(monitor().getStatus()).isEqualTo(MonitorStatus.DOWN);
        Incident incident = openIncident();
        assertThat(incident).isNotNull();
        assertThat(incident.getCause()).isEqualTo("HTTP 503 (expected 200)");
        assertThat(incident.getStartedAt()).isEqualTo(firstCheckTime()); // backdated to the first failure
    }

    @Test
    void a_good_check_resolves_the_open_incident() {
        probe.willReturn(down(), down(), down(), new ProbeResult(200, 40, null));

        runner.runNow(monitorId);
        runner.runNow(monitorId);
        runner.runNow(monitorId);
        long incidentId = openIncident().getId();

        runner.runNow(monitorId);

        assertThat(monitor().getStatus()).isEqualTo(MonitorStatus.UP);
        assertThat(monitor().getConsecutiveFailures()).isZero();
        assertThat(openIncident()).isNull();
        assertThat(incidents.findById(incidentId).orElseThrow().getResolvedAt()).isNotNull();
    }

    @Test
    void slow_response_marks_the_monitor_degraded_without_an_incident() {
        probe.willReturn(new ProbeResult(200, 1200, null));

        CheckResult result = runner.runNow(monitorId).orElseThrow();

        assertThat(result.getOutcome()).isEqualTo(CheckOutcome.DEGRADED);
        assertThat(monitor().getStatus()).isEqualTo(MonitorStatus.DEGRADED);
        assertThat(openIncident()).isNull();
    }

    @Test
    void network_errors_are_recorded_with_their_message() {
        probe.willReturn(new ProbeResult(null, 1000, "Timed out after 1000 ms"));

        CheckResult result = runner.runNow(monitorId).orElseThrow();

        assertThat(result.getOutcome()).isEqualTo(CheckOutcome.DOWN);
        assertThat(result.getStatusCode()).isNull();
        assertThat(result.getErrorMessage()).isEqualTo("Timed out after 1000 ms");
    }

    @Test
    void deleting_a_monitor_removes_its_history() {
        probe.willReturn(down(), down(), down());
        runner.runNow(monitorId);
        runner.runNow(monitorId);
        runner.runNow(monitorId);

        commands.delete(monitorId);

        assertThat(checks.findByMonitorIdOrderByCheckedAtDesc(monitorId, PageRequest.of(0, 10))).isEmpty();
        assertThat(incidents.findFirstByMonitorIdAndResolvedAtIsNull(monitorId)).isEmpty();
    }

    @Test
    void checking_a_deleted_monitor_returns_empty() {
        commands.delete(monitorId);

        assertThat(runner.runNow(monitorId)).isEmpty();
    }

    private static ProbeResult down() {
        return new ProbeResult(503, 35, null);
    }

    private Monitor monitor() {
        return monitors.findById(monitorId).orElseThrow();
    }

    private Incident openIncident() {
        return incidents.findFirstByMonitorIdAndResolvedAtIsNull(monitorId).orElse(null);
    }

    private Instant firstCheckTime() {
        List<CheckResult> recent = checks.findByMonitorIdOrderByCheckedAtDesc(monitorId, PageRequest.of(0, 10));
        return recent.get(recent.size() - 1).getCheckedAt();
    }
}
