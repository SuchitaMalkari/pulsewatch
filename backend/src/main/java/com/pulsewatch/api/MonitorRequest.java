package com.pulsewatch.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for creating or updating a monitor. Only name and url are required; the rest fall
 * back to sensible defaults.
 */
public record MonitorRequest(
        @NotBlank(message = "is required")
        @Size(max = 100, message = "must be at most 100 characters")
        String name,

        @NotBlank(message = "is required")
        @Size(max = 2048, message = "must be at most 2048 characters")
        @Pattern(regexp = "^https?://[^\\s/$.?#][^\\s]*$", message = "must be a valid http:// or https:// URL")
        String url,

        @Min(value = 10, message = "must be at least 10 seconds")
        @Max(value = 3600, message = "must be at most 3600 seconds")
        Integer intervalSeconds,

        @Min(value = 100, message = "must be a valid HTTP status")
        @Max(value = 599, message = "must be a valid HTTP status")
        Integer expectedStatus,

        @Min(value = 50, message = "must be at least 50 ms")
        @Max(value = 60000, message = "must be at most 60000 ms")
        Integer latencyThresholdMs,

        @Min(value = 1, message = "must be at least 1")
        @Max(value = 10, message = "must be at most 10")
        Integer failureThreshold) {

    public static final int DEFAULT_INTERVAL_SECONDS = 30;
    public static final int DEFAULT_EXPECTED_STATUS = 200;
    public static final int DEFAULT_LATENCY_THRESHOLD_MS = 1000;
    public static final int DEFAULT_FAILURE_THRESHOLD = 3;

    public int intervalOrDefault() {
        return intervalSeconds != null ? intervalSeconds : DEFAULT_INTERVAL_SECONDS;
    }

    public int expectedStatusOrDefault() {
        return expectedStatus != null ? expectedStatus : DEFAULT_EXPECTED_STATUS;
    }

    public int latencyThresholdOrDefault() {
        return latencyThresholdMs != null ? latencyThresholdMs : DEFAULT_LATENCY_THRESHOLD_MS;
    }

    public int failureThresholdOrDefault() {
        return failureThreshold != null ? failureThreshold : DEFAULT_FAILURE_THRESHOLD;
    }
}
