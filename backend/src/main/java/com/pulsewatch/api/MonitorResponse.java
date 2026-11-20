package com.pulsewatch.api;

import java.time.Instant;
import java.util.List;

/**
 * A monitor plus the numbers the dashboard shows for it. {@code status} is PAUSED for paused
 * monitors, otherwise PENDING, UP, DEGRADED or DOWN.
 */
public record MonitorResponse(
        long id,
        String name,
        String url,
        int intervalSeconds,
        int expectedStatus,
        int latencyThresholdMs,
        int failureThreshold,
        boolean paused,
        String status,
        Instant lastCheckedAt,
        Long lastLatencyMs,
        Double uptime24h,
        Long p95LatencyMs,
        List<Long> recentLatencies,
        Long openIncidentId,
        Instant createdAt) {
}
