package com.pulsewatch.check;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/** Real probe built on the JDK HTTP client. The response body is discarded; only status and timing matter. */
@Component
public class JdkHttpProbe implements HttpProbe {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public ProbeResult probe(String url, Duration timeout) {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("User-Agent", "PulseWatch/1.0 (+uptime monitor)")
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return new ProbeResult(response.statusCode(), elapsedMs(start), null);
        } catch (HttpTimeoutException e) {
            return new ProbeResult(null, elapsedMs(start), "Timed out after " + timeout.toMillis() + " ms");
        } catch (IOException e) {
            return new ProbeResult(null, elapsedMs(start), describe(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProbeResult(null, elapsedMs(start), "Check was interrupted");
        } catch (IllegalArgumentException e) {
            return new ProbeResult(null, elapsedMs(start), "Invalid URL: " + e.getMessage());
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static String describe(IOException e) {
        String message = e.getMessage();
        String type = e.getClass().getSimpleName();
        return message == null || message.isBlank() ? type : type + ": " + message;
    }
}
