package tn.tt.smartops.common.exception;

import java.util.List;

/** Conflit de planning, de doublon ou de réservation (HTTP 409). */
public class ConflictException extends RuntimeException {
    private final List<String> details;

    public ConflictException(String message) {
        this(message, List.of());
    }

    public ConflictException(String message, List<String> details) {
        super(message);
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }
}
