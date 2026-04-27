package com.heimdallr.monitor.common.domain.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        ErrorCode code,
        boolean success,
        String message,
        T data,
        String requestId,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>(ErrorCode.OK, true, "success", data, requestId, OffsetDateTime.now());
    }

    public static ApiResponse<Void> error(ErrorCode code, String message, String requestId) {
        return new ApiResponse<>(code, false, message, null, requestId, OffsetDateTime.now());
    }
}
