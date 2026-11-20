package com.pulsewatch.check;

import com.pulsewatch.domain.CheckOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LatencyStatsTest {

    @Test
    void no_data_gives_null_rather_than_a_misleading_zero() {
        assertThat(LatencyStats.uptimePercent(List.of())).isNull();
        assertThat(LatencyStats.percentile(List.of(), 95)).isNull();
    }

    @Test
    void degraded_checks_count_as_up_for_uptime() {
        var outcomes = List.of(CheckOutcome.UP, CheckOutcome.DEGRADED, CheckOutcome.DOWN, CheckOutcome.UP);

        assertThat(LatencyStats.uptimePercent(outcomes)).isEqualTo(75.0);
    }

    @Test
    void uptime_is_rounded_to_two_decimals() {
        var outcomes = List.of(CheckOutcome.UP, CheckOutcome.UP, CheckOutcome.DOWN);

        assertThat(LatencyStats.uptimePercent(outcomes)).isEqualTo(66.67);
    }

    @Test
    void p95_uses_nearest_rank() {
        List<Long> oneToHundred = LongStream.rangeClosed(1, 100).boxed().toList();

        assertThat(LatencyStats.percentile(oneToHundred, 95)).isEqualTo(95L);
        assertThat(LatencyStats.percentile(List.of(10L, 20L, 30L), 95)).isEqualTo(30L);
        assertThat(LatencyStats.percentile(List.of(42L), 95)).isEqualTo(42L);
    }

    @Test
    void p95_ignores_input_order() {
        assertThat(LatencyStats.percentile(List.of(900L, 10L, 30L, 20L), 50)).isEqualTo(20L);
    }

    @Test
    void invalid_percentile_is_rejected() {
        assertThatThrownBy(() -> LatencyStats.percentile(List.of(1L), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
