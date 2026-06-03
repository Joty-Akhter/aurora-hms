package com.easyops.hr.integration;

/**
 * Holds the current HTTP request's integration correlation id (INT-42).
 * Set by {@link IntegrationCorrelationFilter} and cleared after each request.
 */
public final class IntegrationCorrelationIdHolder {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private IntegrationCorrelationIdHolder() {}

    public static void set(String correlationId) {
        CURRENT.set(correlationId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
