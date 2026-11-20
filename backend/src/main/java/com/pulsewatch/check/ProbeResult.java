package com.pulsewatch.check;

/**
 * Raw result of one HTTP request.
 *
 * @param statusCode HTTP status, or null when no response arrived (timeout, DNS failure, refused connection)
 * @param latencyMs  time until the response headers arrived, or until the request failed
 * @param error      why the request failed, or null when a response arrived
 */
public record ProbeResult(Integer statusCode, long latencyMs, String error) {
}
