package com.heimdallr.monitor.common.security;

import java.util.Optional;

public final class RequestUserContext {
    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private RequestUserContext() {
    }

    public static void set(CurrentUser user) {
        CURRENT_USER.set(user);
    }

    public static Optional<CurrentUser> current() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static CurrentUser requireCurrent() {
        return current().orElseThrow(() -> new IllegalStateException("Current user is not available"));
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
