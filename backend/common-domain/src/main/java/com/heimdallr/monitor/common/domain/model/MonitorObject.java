package com.heimdallr.monitor.common.domain.model;

import java.util.List;
import java.util.Map;

public record MonitorObject(
        String id,
        String code,
        String name,
        String objectType,
        String environment,
        String businessLine,
        List<String> ownerUserIds,
        List<String> applicationIds,
        List<String> serverIds,
        String healthStatus,
        String accessStatus,
        Map<String, String> keyMetrics
) {
}
