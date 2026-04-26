package com.heimdallr.monitor.common.domain.model;

import java.util.List;
import java.util.Set;

public record UserInfo(
        String id,
        String username,
        String displayName,
        List<RoleInfo> roles,
        Set<String> businessLines,
        Set<String> menus
) {
    public boolean hasPermission(String permission) {
        return roles.stream().anyMatch(role -> role.permissions().contains(permission));
    }
}
