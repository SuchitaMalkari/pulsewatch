package com.pulsewatch.check;

import com.pulsewatch.domain.CheckOutcome;

import java.util.Collection;
import java.util.List;

/** Uptime and latency percentile math for the dashboard. */
public final class LatencyStats {

    private LatencyStats() {
    }

    /** Percentage of checks that got a correct response (UP or DEGRADED), or null with no data. */
    public static Double uptimePercent(Collection<CheckOutcome> outcomes) {
        if (outcomes.isEmpty()) {
            return null;
        }
        long good = outcomes.stream().filter(o -> o != CheckOutcome.DOWN).count();
        return Math.round(good * 10000.0 / outcomes.size()) / 100.0; // two decimal places
    }

    /** Nearest-rank percentile (e.g. 95 for p95), or null with no data. */
    public static Long percentile(Collection<Long> latencies, int percentile) {
        if (latencies.isEmpty()) {
            return null;
        }
        if (percentile < 1 || percentile > 100) {
            throw new IllegalArgumentException("percentile must be between 1 and 100");
        }
        List<Long> sorted = latencies.stream().sorted().toList();
        int rank = (int) Math.ceil(percentile / 100.0 * sorted.size());
        return sorted.get(rank - 1);
    }
}
