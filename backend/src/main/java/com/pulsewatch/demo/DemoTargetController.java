package com.pulsewatch.demo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fake services for the demo, so PulseWatch shows real incidents without depending on the
 * internet: one healthy, one slow and one that has a 45-second outage every 3 minutes.
 */
@RestController
@RequestMapping("/demo")
@ConditionalOnProperty(name = "pulsewatch.demo.enabled", havingValue = "true")
public class DemoTargetController {

    static final long OUTAGE_CYCLE_SECONDS = 180;
    static final long OUTAGE_LENGTH_SECONDS = 45;

    @GetMapping("/healthy")
    public String healthy() {
        pause(20, 90);
        return "ok";
    }

    @GetMapping("/slow")
    public String slow() {
        pause(250, 750);
        return "ok, eventually";
    }

    @GetMapping("/flaky")
    public ResponseEntity<String> flaky() {
        long secondInCycle = Instant.now().getEpochSecond() % OUTAGE_CYCLE_SECONDS;
        if (secondInCycle < OUTAGE_LENGTH_SECONDS) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("down for maintenance");
        }
        pause(30, 150);
        return ResponseEntity.ok("ok");
    }

    private static void pause(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
