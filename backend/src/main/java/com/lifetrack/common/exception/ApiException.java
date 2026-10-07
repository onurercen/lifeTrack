package com.lifetrack.common.exception;

import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Map;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> fieldErrors;
    private final Duration retryAfter;

    public ApiException(HttpStatus status, String message) {
        this(status, message, Map.of(), null);
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.fieldErrors = Map.of();
        this.retryAfter = null;
    }

    private ApiException(HttpStatus status, String message, Map<String, String> fieldErrors, Duration retryAfter) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
        this.retryAfter = retryAfter;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** Shown next to the matching form fields, like bean validation errors. */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    /** Sent as the Retry-After header when set. */
    public Duration getRetryAfter() {
        return retryAfter;
    }

    /** A 400 for a single field, in the same shape as a bean validation error. */
    public static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, Map.of(field, message), null);
    }

    public static ApiException tooManyRequests(String message, Duration retryAfter) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message, Map.of(), retryAfter);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}
