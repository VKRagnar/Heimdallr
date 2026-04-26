package com.heimdallr.monitor.common.domain.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        ErrorCode code,
        String message,
        T data,
        String requestId,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>(ErrorCode.OK, "success", data, requestId, OffsetDateTime.now());
    }

    public static ApiResponse<Void> error(ErrorCode code, String message, String requestId) {
        return new ApiResponse<>(code, message, null, requestId, OffsetDateTime.now());
    }
}
