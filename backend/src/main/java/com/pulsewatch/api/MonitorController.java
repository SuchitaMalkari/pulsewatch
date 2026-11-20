package com.pulsewatch.api;

import com.pulsewatch.check.CheckRunner;
import com.pulsewatch.command.MonitorCommandService;
import com.pulsewatch.query.MonitorQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/monitors")
public class MonitorController {

    private final MonitorQueryService queries;
    private final MonitorCommandService commands;
    private final CheckRunner checkRunner;

    public MonitorController(MonitorQueryService queries, MonitorCommandService commands, CheckRunner checkRunner) {
        this.queries = queries;
        this.commands = commands;
        this.checkRunner = checkRunner;
    }

    @GetMapping
    public List<MonitorResponse> list() {
        return queries.list();
    }

    @GetMapping("/{id}")
    public MonitorResponse get(@PathVariable long id) {
        return queries.get(id);
    }

    @PostMapping
    public ResponseEntity<MonitorResponse> create(@Valid @RequestBody MonitorRequest request) {
        long id = commands.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(queries.get(id));
    }

    @PutMapping("/{id}")
    public MonitorResponse update(@PathVariable long id, @Valid @RequestBody MonitorRequest request) {
        commands.update(id, request);
        return queries.get(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        commands.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    public MonitorResponse pause(@PathVariable long id) {
        commands.setPaused(id, true);
        return queries.get(id);
    }

    @PostMapping("/{id}/resume")
    public MonitorResponse resume(@PathVariable long id) {
        commands.setPaused(id, false);
        return queries.get(id);
    }

    /** Runs a check immediately and returns its result. */
    @PostMapping("/{id}/check")
    public CheckResponse checkNow(@PathVariable long id) {
        return checkRunner.runNow(id)
                .map(CheckResponse::from)
                .orElseThrow(() -> NotFoundException.monitor(id));
    }

    @GetMapping("/{id}/checks")
    public List<CheckResponse> checks(@PathVariable long id, @RequestParam(defaultValue = "60") int limit) {
        return queries.recentChecks(id, limit);
    }
}
