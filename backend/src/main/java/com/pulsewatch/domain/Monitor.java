package com.pulsewatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;

/**
 * An HTTP endpoint that PulseWatch checks on a schedule.
 *
 * <p>{@code @DynamicUpdate} makes Hibernate write only the columns that changed. A background
 * check (which changes status fields) and a user edit (which changes settings or pause) can
 * therefore save at the same moment without one silently overwriting the other.
 */
@Entity
@DynamicUpdate
@Table(name = "monitors")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false)
    private int intervalSeconds;

    @Column(nullable = false)
    private int expectedStatus;

    @Column(nullable = false)
    private int latencyThresholdMs;

    @Column(nullable = false)
    private int failureThreshold;

    @Column(nullable = false)
    private boolean paused;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MonitorStatus status = MonitorStatus.PENDING;

    @Column(nullable = false)
    private int consecutiveFailures;

    private Instant lastCheckedAt;

    private Long lastLatencyMs;

    @Column(nullable = false)
    private Instant createdAt;

    protected Monitor() {
        // for JPA
    }

    public Monitor(String name, String url, int intervalSeconds, int expectedStatus,
                   int latencyThresholdMs, int failureThreshold, Instant createdAt) {
        updateSettings(name, url, intervalSeconds, expectedStatus, latencyThresholdMs, failureThreshold);
        this.createdAt = createdAt;
    }

    public void updateSettings(String name, String url, int intervalSeconds, int expectedStatus,
                               int latencyThresholdMs, int failureThreshold) {
        this.name = name;
        this.url = url;
        this.intervalSeconds = intervalSeconds;
        this.expectedStatus = expectedStatus;
        this.latencyThresholdMs = latencyThresholdMs;
        this.failureThreshold = failureThreshold;
    }

    /** Applies the state machine's decision after a check. */
    public void recordCheck(MonitorStatus newStatus, int newConsecutiveFailures, Instant checkedAt, long latencyMs) {
        this.status = newStatus;
        this.consecutiveFailures = newConsecutiveFailures;
        this.lastCheckedAt = checkedAt;
        this.lastLatencyMs = latencyMs;
    }

    public void pause() {
        this.paused = true;
    }

    public void resume() {
        this.paused = false;
    }

    /** True when the monitor is active and its interval has passed since the last check. */
    public boolean isDue(Instant now) {
        return !paused && (lastCheckedAt == null || !lastCheckedAt.plusSeconds(intervalSeconds).isAfter(now));
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public int getIntervalSeconds() { return intervalSeconds; }
    public int getExpectedStatus() { return expectedStatus; }
    public int getLatencyThresholdMs() { return latencyThresholdMs; }
    public int getFailureThreshold() { return failureThreshold; }
    public boolean isPaused() { return paused; }
    public MonitorStatus getStatus() { return status; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public Long getLastLatencyMs() { return lastLatencyMs; }
    public Instant getCreatedAt() { return createdAt; }
}
