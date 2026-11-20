package com.pulsewatch.domain;

/** Current health of a monitor, decided from its recent checks. */
public enum MonitorStatus {
    /** Created but not yet confirmed up or down. */
    PENDING,
    UP,
    /** Responding correctly but slower than its latency threshold. */
    DEGRADED,
    /** Failed enough consecutive checks to open an incident. */
    DOWN
}
