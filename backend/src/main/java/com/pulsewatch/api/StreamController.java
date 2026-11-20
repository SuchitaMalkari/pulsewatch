package com.pulsewatch.api;

import com.pulsewatch.live.LiveUpdateBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Live updates for the dashboard. Events: "monitor", "monitor-deleted", "incident". */
@RestController
public class StreamController {

    private final LiveUpdateBroadcaster broadcaster;

    public StreamController(LiveUpdateBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(path = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return broadcaster.subscribe();
    }
}
