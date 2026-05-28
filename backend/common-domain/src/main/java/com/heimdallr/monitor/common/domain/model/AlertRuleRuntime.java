package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;

public record AlertRuleRuntime(
        String ruleId,
        OffsetDateTime lastEvaluatedAt,
        OffsetDateTime nextEvaluateAt,
        String lastStatus,
        Double lastValue,
        String lastError,
        Long evaluationDurationMs,
        OffsetDateTime updatedAt
) {
}
