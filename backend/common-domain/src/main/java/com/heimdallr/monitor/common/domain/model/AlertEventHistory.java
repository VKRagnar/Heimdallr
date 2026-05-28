package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;

public record AlertEventHistory(
        String id,
        String eventId,
        String fromStatus,
        String toStatus,
        String action,
        String operatorUserId,
        String message,
        OffsetDateTime operatedAt
) {
}
