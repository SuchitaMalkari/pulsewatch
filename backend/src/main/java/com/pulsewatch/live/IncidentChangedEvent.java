package com.pulsewatch.live;

/** Published inside a transaction when an incident opens or resolves. */
public record IncidentChangedEvent(long incidentId) {
}
