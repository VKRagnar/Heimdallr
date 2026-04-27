package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;

public record DataSourceConfig(
        String id,
        String name,
        String type,
        String environment,
        String baseUrl,
        String healthCheckPath,
        String authType,
        String secretRef,
        int timeoutSeconds,
        int retryCount,
        int rateLimitQps,
        String status,
        OffsetDateTime lastCheckAt,
        OffsetDateTime lastSuccessAt,
        String lastErrorCode,
        String lastErrorMessage
) {
}
