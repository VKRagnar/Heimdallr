package com.datamonitor.api.controller;

import com.datamonitor.api.fixture.InMemoryMonitorData;
import com.datamonitor.common.domain.api.PageResult;
import com.datamonitor.common.domain.model.RoleInfo;
import com.datamonitor.common.domain.model.UserInfo;
import com.datamonitor.common.security.RequestUserContext;
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
