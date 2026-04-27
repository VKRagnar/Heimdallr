package com.heimdallr.monitor.api.adapter;

import com.heimdallr.monitor.common.domain.model.MetricSeries;
import java.time.OffsetDateTime;

public interface PrometheusMetricAdapter {
    MetricSeries query(String metricCode, String objectId, OffsetDateTime from, OffsetDateTime to);
}
