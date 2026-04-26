package com.datamonitor.common.domain.model;

public record ApplicationInstance(
        String id,
        String applicationId,
        String serverId,
        String hostname,
        String environment,
        String status
) {
}
