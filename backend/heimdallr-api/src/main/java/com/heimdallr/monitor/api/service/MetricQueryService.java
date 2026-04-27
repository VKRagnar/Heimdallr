package com.heimdallr.monitor.api.service;

import com.heimdallr.monitor.api.fixture.InMemoryMonitorData;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.DefaultMetricMapping;
import com.heimdallr.monitor.common.domain.model.MetricDefinition;
import com.heimdallr.monitor.common.domain.model.MetricSeries;
import com.heimdallr.monitor.common.security.CurrentUser;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class MetricQueryService {
    private final InMemoryMonitorData data;

    public MetricQueryService(InMemoryMonitorData data) {
        this.data = data;
    }

    public PageResult<MetricDefinition> definitions(CurrentUser currentUser, String objectType) {
        data.requirePermission(currentUser, "metrics:read");
        return PageResult.all(data.metricDefinitions(currentUser, objectType));
    }

    public PageResult<DefaultMetricMapping> defaultMappings(CurrentUser currentUser, String objectType) {
        data.requirePermission(currentUser, "metrics:read");
        return PageResult.all(data.defaultMetricMappings(objectType));
    }

    public MetricSeries query(CurrentUser currentUser, String metricCode, String objectId, OffsetDateTime from, OffsetDateTime to) {
        data.requirePermission(currentUser, "metrics:read");
        return data.queryMetric(currentUser, metricCode, objectId, from, to);
    }
}
