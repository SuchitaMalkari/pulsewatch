package com.pulsewatch.live;

import com.pulsewatch.query.MonitorQueryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Pushes changes to dashboards only after the transaction commits, so a browser never sees
 * data that was later rolled back.
 */
@Component
public class LiveUpdateListener {

    private final MonitorQueryService queries;
    private final LiveUpdateBroadcaster broadcaster;

    public LiveUpdateListener(MonitorQueryService queries, LiveUpdateBroadcaster broadcaster) {
        this.queries = queries;
        this.broadcaster = broadcaster;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onMonitorChanged(MonitorChangedEvent event) {
        if (broadcaster.subscriberCount() == 0) {
            return;
        }
        queries.find(event.monitorId()).ifPresent(monitor -> broadcaster.broadcast("monitor", monitor));
    }

    @TransactionalEventListener
    public void onMonitorDeleted(MonitorDeletedEvent event) {
        broadcaster.broadcast("monitor-deleted", Map.of("id", event.monitorId()));
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onIncidentChanged(IncidentChangedEvent event) {
        if (broadcaster.subscriberCount() == 0) {
            return;
        }
        queries.incident(event.incidentId()).ifPresent(incident -> broadcaster.broadcast("incident", incident));
    }
}
