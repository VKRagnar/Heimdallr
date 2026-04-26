package com.datamonitor.common.security;

import com.datamonitor.common.domain.model.DataScope;
import com.datamonitor.common.domain.model.UserInfo;

public record CurrentUser(UserInfo user, DataScope dataScope, String token) {
    public boolean hasPermission(String permission) {
        return user.hasPermission(permission);
    }
}
