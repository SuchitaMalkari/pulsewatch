package com.pulsewatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

/** A period during which a monitor was DOWN. Open while resolvedAt is null. */
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long monitorId;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant resolvedAt;

    @Column(nullable = false, length = 500)
    private String cause;

    protected Incident() {
        // for JPA
    }

    public Incident(Long monitorId, Instant startedAt, String cause) {
        this.monitorId = monitorId;
        this.startedAt = startedAt;
        this.cause = cause;
    }

    public void resolve(Instant at) {
        if (resolvedAt == null) {
            resolvedAt = at;
        }
    }

    public boolean isOpen() {
        return resolvedAt == null;
    }

    /** How long the outage lasted, or has lasted so far if still open. */
    public Duration duration(Instant now) {
        return Duration.between(startedAt, resolvedAt != null ? resolvedAt : now);
    }

    public Long getId() { return id; }
    public Long getMonitorId() { return monitorId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getCause() { return cause; }
}
