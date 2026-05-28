package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;

public record AlertRule(
        String id,
        String name,
        String objectId,
        String objectName,
        String metricCode,
        String operator,
        double threshold,
        int windowSeconds,
        int durationSeconds,
        int evaluationIntervalSeconds,
        String severity,
        boolean enabled,
        String businessLine,
        String appId,
        String onCallGroupId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
