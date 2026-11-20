package com.pulsewatch.check;

import com.pulsewatch.domain.CheckOutcome;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutcomeClassifierTest {

    @Test
    void expected_status_within_threshold_is_up() {
        assertThat(OutcomeClassifier.classify(new ProbeResult(200, 120, null), 200, 500))
                .isEqualTo(CheckOutcome.UP);
    }

    @Test
    void expected_status_but_slow_is_degraded() {
        assertThat(OutcomeClassifier.classify(new ProbeResult(200, 900, null), 200, 500))
                .isEqualTo(CheckOutcome.DEGRADED);
    }

    @Test
    void latency_exactly_at_threshold_is_still_up() {
        assertThat(OutcomeClassifier.classify(new ProbeResult(200, 500, null), 200, 500))
                .isEqualTo(CheckOutcome.UP);
    }

    @Test
    void unexpected_status_is_down() {
        assertThat(OutcomeClassifier.classify(new ProbeResult(503, 40, null), 200, 500))
                .isEqualTo(CheckOutcome.DOWN);
    }

    @Test
    void no_response_is_down() {
        assertThat(OutcomeClassifier.classify(new ProbeResult(null, 5000, "Timed out"), 200, 500))
                .isEqualTo(CheckOutcome.DOWN);
    }

    @Test
    void a_monitor_can_expect_a_non_200_status() {
        assertThat(OutcomeClassifier.classify(new ProbeResult(401, 30, null), 401, 500))
                .isEqualTo(CheckOutcome.UP);
    }
}
