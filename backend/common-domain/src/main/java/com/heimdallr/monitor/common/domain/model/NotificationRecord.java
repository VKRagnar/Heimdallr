package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;

public record NotificationRecord(
        String id,
        String eventId,
        String ruleId,
        String channelType,
        String receiver,
        String status,
        int retryCount,
        String failureReason,
        OffsetDateTime nextRetryAt,
        OffsetDateTime sentAt,
        OffsetDateTime createdAt
) {
}
