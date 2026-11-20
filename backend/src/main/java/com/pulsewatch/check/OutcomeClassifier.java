package com.pulsewatch.check;

import com.pulsewatch.domain.CheckOutcome;

/** Turns a raw probe result into UP, DEGRADED or DOWN for one monitor's rules. */
public final class OutcomeClassifier {

    private OutcomeClassifier() {
    }

    public static CheckOutcome classify(ProbeResult probe, int expectedStatus, int latencyThresholdMs) {
        if (probe.statusCode() == null || probe.statusCode() != expectedStatus) {
            return CheckOutcome.DOWN;
        }
        if (probe.latencyMs() > latencyThresholdMs) {
            return CheckOutcome.DEGRADED;
        }
        return CheckOutcome.UP;
    }
}
