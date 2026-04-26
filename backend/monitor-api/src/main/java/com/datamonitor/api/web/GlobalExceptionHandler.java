package com.datamonitor.api.web;

import com.datamonitor.common.domain.api.ApiResponse;
import com.datamonitor.common.domain.api.ErrorCode;
import com.datamonitor.common.domain.exception.ApiException;
import com.datamonitor.common.observability.RequestId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return ResponseEntity
                .status(ex.httpStatus())
                .body(ApiResponse.error(ex.code(), ex.getMessage(), RequestId.currentOrFallback()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED, "Request validation failed", RequestId.currentOrFallback()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected API exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, "Internal server error", RequestId.currentOrFallback()));
    }
}
