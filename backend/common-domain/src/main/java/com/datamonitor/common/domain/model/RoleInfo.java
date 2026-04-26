package com.datamonitor.common.domain.model;

import java.util.Set;

public record RoleInfo(
        String id,
        String code,
        String name,
        Set<String> permissions
) {
}
