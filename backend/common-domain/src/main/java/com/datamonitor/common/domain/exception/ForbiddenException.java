package com.datamonitor.common.domain.exception;

import com.datamonitor.common.domain.api.ErrorCode;

public class ForbiddenException extends ApiException {
    public ForbiddenException(ErrorCode code, String message) {
        super(code, 403, message);
    }
}
