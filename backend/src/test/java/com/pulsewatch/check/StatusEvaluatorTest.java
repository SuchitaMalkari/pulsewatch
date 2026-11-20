package com.pulsewatch.check;

import com.pulsewatch.domain.CheckOutcome;
import com.pulsewatch.domain.MonitorStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class StatusEvaluatorTest {

    @Test
    void single_failure_below_threshold_keeps_current_status() {
        var t = StatusEvaluator.evaluate(MonitorStatus.UP, 0, 3, CheckOutcome.DOWN);

        assertThat(t.status()).isEqualTo(MonitorStatus.UP);
        assertThat(t.consecutiveFailures()).isEqualTo(1);
        assertThat(t.openIncident()).isFalse();
    }

    @Test
    void reaching_the_threshold_marks_down_and_opens_an_incident() {
        var t = StatusEvaluator.evaluate(MonitorStatus.UP, 2, 3, CheckOutcome.DOWN);

        assertThat(t.status()).isEqualTo(MonitorStatus.DOWN);
        assertThat(t.consecutiveFailures()).isEqualTo(3);
        assertThat(t.openIncident()).isTrue();
        assertThat(t.resolveIncident()).isFalse();
    }

    @Test
    void further_failures_while_down_do_not_open_another_incident() {
        var t = StatusEvaluator.evaluate(MonitorStatus.DOWN, 5, 3, CheckOutcome.DOWN);

        assertThat(t.status()).isEqualTo(MonitorStatus.DOWN);
        assertThat(t.consecutiveFailures()).isEqualTo(6);
        assertThat(t.openIncident()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = CheckOutcome.class, names = {"UP", "DEGRADED"})
    void a_good_check_after_an_outage_resolves_the_incident(CheckOutcome outcome) {
        var t = StatusEvaluator.evaluate(MonitorStatus.DOWN, 4, 3, outcome);

        assertThat(t.status().name()).isEqualTo(outcome.name());
        assertThat(t.consecutiveFailures()).isZero();
        assertThat(t.resolveIncident()).isTrue();
    }

    @Test
    void a_good_check_resets_the_failure_count_without_resolving_anything() {
        var t = StatusEvaluator.evaluate(MonitorStatus.UP, 2, 3, CheckOutcome.UP);

        assertThat(t.consecutiveFailures()).isZero();
        assertThat(t.resolveIncident()).isFalse();
    }

    @Test
    void new_monitor_stays_pending_until_the_threshold_is_reached() {
        var t = StatusEvaluator.evaluate(MonitorStatus.PENDING, 0, 3, CheckOutcome.DOWN);

        assertThat(t.status()).isEqualTo(MonitorStatus.PENDING);
    }

    @Test
    void threshold_below_one_is_treated_as_one() {
        var t = StatusEvaluator.evaluate(MonitorStatus.UP, 0, 0, CheckOutcome.DOWN);

        assertThat(t.status()).isEqualTo(MonitorStatus.DOWN);
        assertThat(t.openIncident()).isTrue();
    }
}
