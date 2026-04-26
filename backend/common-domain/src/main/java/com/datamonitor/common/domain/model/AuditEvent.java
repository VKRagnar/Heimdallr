package com.datamonitor.common.domain.model;

import java.time.OffsetDateTime;

public record AuditEvent(
        String id,
        String actorUserId,
        String action,
        String targetType,
        String targetId,
        String result,
        OffsetDateTime occurredAt
) {
}
