package com.heimdallr.monitor.common.domain.model;

import java.util.Map;

public record DefaultMetricMapping(
        String id,
        String objectType,
        String metricCode,
        String sourceType,
        String externalMetric,
        String queryTemplate,
        String unit,
        Map<String, String> defaultLabels
) {
}
