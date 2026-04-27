package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record MetricSeries(
        String metricCode,
        String objectId,
        String objectName,
        String unit,
        String sourceId,
        OffsetDateTime from,
        OffsetDateTime to,
        List<MetricSample> samples,
        Map<String, String> labels
) {
    public record MetricSample(
            OffsetDateTime timestamp,
            double value
    ) {
    }
}
