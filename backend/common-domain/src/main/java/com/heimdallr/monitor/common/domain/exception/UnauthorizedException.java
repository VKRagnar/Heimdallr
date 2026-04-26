package com.heimdallr.monitor.common.domain.exception;

import com.heimdallr.monitor.common.domain.api.ErrorCode;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, 401, message);
    }
}
