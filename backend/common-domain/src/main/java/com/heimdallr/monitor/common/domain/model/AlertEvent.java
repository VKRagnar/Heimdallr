package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;

public record AlertEvent(
        String id,
        String ruleId,
        String ruleName,
        String objectId,
        String objectName,
        String metricCode,
        String severity,
        String status,
        double triggerValue,
        double threshold,
        String operator,
        String assigneeUserId,
        String closeReason,
        OffsetDateTime triggeredAt,
        OffsetDateTime notifiedAt,
        OffsetDateTime acknowledgedAt,
        OffsetDateTime processingAt,
        OffsetDateTime recoveredAt,
        OffsetDateTime closedAt,
        OffsetDateTime updatedAt
) {
}
