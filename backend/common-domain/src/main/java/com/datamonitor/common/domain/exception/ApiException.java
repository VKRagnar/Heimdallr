package com.datamonitor.common.domain.exception;

import com.datamonitor.common.domain.api.ErrorCode;

public class ApiException extends RuntimeException {
    private final ErrorCode code;
    private final int httpStatus;

    public ApiException(ErrorCode code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public ErrorCode code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
