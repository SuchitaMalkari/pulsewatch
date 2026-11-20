package com.pulsewatch.command;

import com.pulsewatch.api.MonitorRequest;
import com.pulsewatch.api.NotFoundException;
import com.pulsewatch.domain.Monitor;
import com.pulsewatch.domain.MonitorRepository;
import com.pulsewatch.live.MonitorChangedEvent;
import com.pulsewatch.live.MonitorDeletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Write side: creates, edits, pauses and deletes monitors. */
@Service
@Transactional
public class MonitorCommandService {

    private final MonitorRepository monitors;
    private final ApplicationEventPublisher events;

    public MonitorCommandService(MonitorRepository monitors, ApplicationEventPublisher events) {
        this.monitors = monitors;
        this.events = events;
    }

    public long create(MonitorRequest request) {
        Monitor monitor = monitors.save(new Monitor(
                request.name().trim(),
                request.url().trim(),
                request.intervalOrDefault(),
                request.expectedStatusOrDefault(),
                request.latencyThresholdOrDefault(),
                request.failureThresholdOrDefault(),
                Instant.now()));
        events.publishEvent(new MonitorChangedEvent(monitor.getId()));
        return monitor.getId();
    }

    public void update(long id, MonitorRequest request) {
        Monitor monitor = load(id);
        monitor.updateSettings(
                request.name().trim(),
                request.url().trim(),
                request.intervalOrDefault(),
                request.expectedStatusOrDefault(),
                request.latencyThresholdOrDefault(),
                request.failureThresholdOrDefault());
        events.publishEvent(new MonitorChangedEvent(id));
    }

    public void setPaused(long id, boolean paused) {
        Monitor monitor = load(id);
        if (paused) {
            monitor.pause();
        } else {
            monitor.resume();
        }
        events.publishEvent(new MonitorChangedEvent(id));
    }

    /** Check results and incidents are removed by ON DELETE CASCADE in the schema. */
    public void delete(long id) {
        monitors.delete(load(id));
        events.publishEvent(new MonitorDeletedEvent(id));
    }

    private Monitor load(long id) {
        return monitors.findById(id).orElseThrow(() -> NotFoundException.monitor(id));
    }
}
