package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;
import java.util.Map;

public record DataSourceBinding(
        String id,
        String objectId,
        String objectType,
        String sourceId,
        String bindingType,
        Map<String, String> externalLabels,
        Map<String, String> mappingConfig,
        OffsetDateTime lastSeenAt,
        String accessStatus,
        String failureReason
) {
}
