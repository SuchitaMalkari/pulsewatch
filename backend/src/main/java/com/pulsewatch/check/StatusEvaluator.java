package com.pulsewatch.check;

import com.pulsewatch.domain.CheckOutcome;
import com.pulsewatch.domain.MonitorStatus;

/**
 * The monitor state machine, kept free of I/O so every rule is unit tested.
 *
 * <p>A single failed check does not mark a monitor DOWN: it takes {@code failureThreshold}
 * failures in a row. This stops one network blip from paging anyone. The first good check
 * after an outage resolves the incident.
 */
public final class StatusEvaluator {

    private StatusEvaluator() {
    }

    /** What should happen to a monitor after one check. */
    public record Transition(
            MonitorStatus status,
            int consecutiveFailures,
            boolean openIncident,
            boolean resolveIncident) {
    }

    public static Transition evaluate(
            MonitorStatus current,
            int consecutiveFailures,
            int failureThreshold,
            CheckOutcome outcome) {

        int threshold = Math.max(1, failureThreshold);

        if (outcome == CheckOutcome.DOWN) {
            int failures = consecutiveFailures + 1;
            if (current == MonitorStatus.DOWN) {
                return new Transition(MonitorStatus.DOWN, failures, false, false); // outage continues
            }
            if (failures >= threshold) {
                return new Transition(MonitorStatus.DOWN, failures, true, false); // outage confirmed
            }
            return new Transition(current, failures, false, false); // tolerate a blip
        }

        MonitorStatus next = outcome == CheckOutcome.UP ? MonitorStatus.UP : MonitorStatus.DEGRADED;
        return new Transition(next, 0, false, current == MonitorStatus.DOWN);
    }
}
