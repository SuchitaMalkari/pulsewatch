package com.pulsewatch.api;

import com.pulsewatch.domain.CheckResult;

import java.time.Instant;

public record CheckResponse(
        long id,
        Instant checkedAt,
        String outcome,
        Integer statusCode,
        long latencyMs,
        String error) {

    public static CheckResponse from(CheckResult check) {
        return new CheckResponse(check.getId(), check.getCheckedAt(), check.getOutcome().name(),
                check.getStatusCode(), check.getLatencyMs(), check.getErrorMessage());
    }
}
