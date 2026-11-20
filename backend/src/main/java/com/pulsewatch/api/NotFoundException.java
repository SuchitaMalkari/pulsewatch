package com.pulsewatch.api;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException monitor(long id) {
        return new NotFoundException("Monitor " + id + " does not exist");
    }
}
