package com.datamonitor.common.domain.exception;

import com.datamonitor.common.domain.api.ErrorCode;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, 401, message);
    }
}
