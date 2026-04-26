package com.heimdallr.monitor.api.controller;

import com.heimdallr.monitor.api.fixture.InMemoryMonitorData;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.RoleInfo;
import com.heimdallr.monitor.common.domain.model.UserInfo;
import com.heimdallr.monitor.common.security.RequestUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/access")
public class AccessController {
    private final InMemoryMonitorData data;

    public AccessController(InMemoryMonitorData data) {
        this.data = data;
    }

    @GetMapping("/users")
    public PageResult<UserInfo> users() {
        return PageResult.all(data.users(RequestUserContext.requireCurrent()));
    }

    @GetMapping("/roles")
    public PageResult<RoleInfo> roles() {
        return PageResult.all(data.roles(RequestUserContext.requireCurrent()));
    }
}
