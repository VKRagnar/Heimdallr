package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;

public record AlertEvaluationSample(
        String id,
        String ruleId,
        OffsetDateTime evaluatedAt,
        String status,
        Double value,
        double threshold,
        String operator,
        boolean matched,
        String eventId,
        String error,
        Long evaluationDurationMs,
        OffsetDateTime createdAt
) {
}
