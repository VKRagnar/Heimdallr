package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record DataSourceValidationResult(
        String sourceId,
        boolean passed,
        String status,
        OffsetDateTime checkedAt,
        List<ValidationItem> items,
        Map<String, String> sampleLabels,
        String message
) {
    public record ValidationItem(
            String name,
            String status,
            String code,
            String message
    ) {
    }
}
