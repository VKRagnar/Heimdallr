package com.heimdallr.monitor.api.dto;

import com.heimdallr.monitor.common.domain.model.DataSourceConfig;
import java.time.OffsetDateTime;

public record DataSourceConfigResponse(
        String id,
        String name,
        String type,
        String environment,
        String baseUrl,
        String healthCheckPath,
        String authType,
        boolean hasSecretRef,
        int timeoutSeconds,
        int retryCount,
        int rateLimitQps,
        String status,
        OffsetDateTime lastCheckAt,
        OffsetDateTime lastSuccessAt,
        String lastErrorCode,
        String lastErrorMessage
) {
    public static DataSourceConfigResponse from(DataSourceConfig source) {
        return new DataSourceConfigResponse(
                source.id(),
                source.name(),
                source.type(),
                source.environment(),
                source.baseUrl(),
                source.healthCheckPath(),
                source.authType(),
                source.secretRef() != null && !source.secretRef().isBlank(),
                source.timeoutSeconds(),
                source.retryCount(),
                source.rateLimitQps(),
                source.status(),
                source.lastCheckAt(),
                source.lastSuccessAt(),
                source.lastErrorCode(),
                source.lastErrorMessage()
        );
    }
}
