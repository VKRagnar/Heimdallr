package com.datamonitor.common.domain.exception;

import com.datamonitor.common.domain.api.ErrorCode;

public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, 404, message);
    }
}
