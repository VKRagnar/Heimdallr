package com.datamonitor.api.controller;

import com.datamonitor.api.fixture.InMemoryMonitorData;
import com.datamonitor.common.domain.api.PageResult;
import com.datamonitor.common.domain.model.ServerAsset;
import com.datamonitor.common.security.RequestUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/servers")
public class ServerController {
    private final InMemoryMonitorData data;

    public ServerController(InMemoryMonitorData data) {
        this.data = data;
    }

    @GetMapping
    public PageResult<ServerAsset> list() {
        var currentUser = RequestUserContext.requireCurrent();
        data.requirePermission(currentUser, "servers:read");
        return PageResult.all(data.visibleServers(currentUser));
    }
}
