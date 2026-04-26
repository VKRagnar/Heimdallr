package com.heimdallr.monitor.common.security;

import com.heimdallr.monitor.common.domain.model.DataScope;
import com.heimdallr.monitor.common.domain.model.UserInfo;

public record CurrentUser(UserInfo user, DataScope dataScope, String token) {
    public boolean hasPermission(String permission) {
        return user.hasPermission(permission);
    }
}
