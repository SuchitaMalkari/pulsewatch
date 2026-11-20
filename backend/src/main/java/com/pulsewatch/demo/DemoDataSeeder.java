package com.pulsewatch.demo;

import com.pulsewatch.api.MonitorRequest;
import com.pulsewatch.command.MonitorCommandService;
import com.pulsewatch.domain.MonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** On first start with an empty database, adds monitors for the demo services. */
@Component
@ConditionalOnProperty(name = "pulsewatch.demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final MonitorRepository monitors;
    private final MonitorCommandService commands;
    private final int port;

    public DemoDataSeeder(MonitorRepository monitors, MonitorCommandService commands,
                          @Value("${server.port:8080}") int port) {
        this.monitors = monitors;
        this.commands = commands;
        this.port = port;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (monitors.count() > 0) {
            return;
        }
        String base = "http://localhost:" + port + "/demo";
        commands.create(new MonitorRequest("Payments API", base + "/healthy", 10, 200, 300, 3));
        commands.create(new MonitorRequest("Search Service", base + "/slow", 10, 200, 400, 3));
        commands.create(new MonitorRequest("Inventory API", base + "/flaky", 10, 200, 500, 3));
        commands.create(new MonitorRequest("GitHub", "https://github.com", 60, 200, 1500, 3));
        log.info("Demo mode: added 4 monitors. Disable with pulsewatch.demo.enabled=false");
    }
}
