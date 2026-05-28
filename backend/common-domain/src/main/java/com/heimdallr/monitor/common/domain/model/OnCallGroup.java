package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

public record OnCallGroup(
        String id,
        String code,
        String name,
        String businessLine,
        List<String> memberUserIds,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
