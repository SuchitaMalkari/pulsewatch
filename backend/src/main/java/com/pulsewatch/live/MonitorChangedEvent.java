package com.pulsewatch.live;

/** Published inside a transaction whenever a monitor's settings or status change. */
public record MonitorChangedEvent(long monitorId) {
}
