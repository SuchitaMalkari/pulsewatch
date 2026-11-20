package com.pulsewatch.check;

import java.time.Duration;

/** Sends one HTTP request to a monitored URL. An interface so tests can swap in a fake. */
public interface HttpProbe {

    ProbeResult probe(String url, Duration timeout);
}
