package com.pulsewatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One recorded check of a monitor. Rows older than the retention period are deleted daily. */
@Entity
@Table(name = "check_results")
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long monitorId;

    @Column(nullable = false)
    private Instant checkedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckOutcome outcome;

    private Integer statusCode;

    @Column(nullable = false)
    private long latencyMs;

    @Column(length = 500)
    private String errorMessage;

    protected CheckResult() {
        // for JPA
    }

    public CheckResult(Long monitorId, Instant checkedAt, CheckOutcome outcome,
                       Integer statusCode, long latencyMs, String errorMessage) {
        this.monitorId = monitorId;
        this.checkedAt = checkedAt;
        this.outcome = outcome;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public Long getMonitorId() { return monitorId; }
    public Instant getCheckedAt() { return checkedAt; }
    public CheckOutcome getOutcome() { return outcome; }
    public Integer getStatusCode() { return statusCode; }
    public long getLatencyMs() { return latencyMs; }
    public String getErrorMessage() { return errorMessage; }
}
