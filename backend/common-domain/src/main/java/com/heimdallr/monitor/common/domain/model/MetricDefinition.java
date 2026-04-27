package com.heimdallr.monitor.common.domain.model;

import java.util.List;

public record MetricDefinition(
        String code,
        String name,
        String objectType,
        String category,
        String unit,
        String sourceType,
        String defaultQueryTemplate,
        List<String> labels
) {
}
