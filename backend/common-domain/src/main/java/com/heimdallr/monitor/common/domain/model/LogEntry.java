package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;
import java.util.Map;

public record LogEntry(
        String id,
        OffsetDateTime timestamp,
        String applicationId,
        String objectId,
        String environment,
        String level,
        String message,
        String traceId,
        String sourceId,
        Map<String, String> labels
) {
}
