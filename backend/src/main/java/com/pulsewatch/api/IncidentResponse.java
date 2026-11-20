package com.pulsewatch.api;

import java.time.Instant;

public record IncidentResponse(
        long id,
        long monitorId,
        String monitorName,
        Instant startedAt,
        Instant resolvedAt,
        String cause,
        long durationSeconds) {
}
