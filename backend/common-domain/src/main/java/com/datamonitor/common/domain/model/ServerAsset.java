package com.datamonitor.common.domain.model;

import java.util.Set;

public record ServerAsset(
        String id,
        String hostname,
        String ip,
        String environment,
        Set<String> applicationIds,
        String accessStatus
) {
}
