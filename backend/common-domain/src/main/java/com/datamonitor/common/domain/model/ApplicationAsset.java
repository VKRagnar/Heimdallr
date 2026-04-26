package com.datamonitor.common.domain.model;

import java.util.List;

public record ApplicationAsset(
        String id,
        String code,
        String name,
        String businessLine,
        String environment,
        List<String> ownerUserIds,
        String accessStatus
) {
}
