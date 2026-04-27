package com.heimdallr.monitor.api.dto;

import java.time.OffsetDateTime;

public record ApplicationAccessStatusResponse(
        String id,
        String appId,
        String appName,
        String appCode,
        String environment,
        String owner,
        String metricsAccess,
        String traceAccess,
        String logsAccess,
        String healthCheck,
        String agentStatus,
        String accessStatus,
        OffsetDateTime lastVerifiedAt
) {
}
