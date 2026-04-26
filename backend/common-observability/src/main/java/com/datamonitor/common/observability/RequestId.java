package com.datamonitor.common.observability;

import java.util.Optional;

public final class RequestId {
    public static final String HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = "requestId";
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private RequestId() {
    }

    public static void set(String requestId) {
        CURRENT.set(requestId);
    }

    public static Optional<String> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static String currentOrFallback() {
        return current().orElse("unknown");
    }

    public static void clear() {
        CURRENT.remove();
    }
}
