package com.pulsewatch.api;

import com.pulsewatch.query.MonitorQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final MonitorQueryService queries;

    public IncidentController(MonitorQueryService queries) {
        this.queries = queries;
    }

    /** {@code ?open=true} returns only ongoing incidents; otherwise the 50 most recent. */
    @GetMapping
    public List<IncidentResponse> list(@RequestParam(defaultValue = "false") boolean open) {
        return queries.incidents(open);
    }
}
