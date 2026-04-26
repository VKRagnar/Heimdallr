package com.datamonitor.api.controller;

import com.datamonitor.api.fixture.InMemoryMonitorData;
import com.datamonitor.common.domain.api.PageResult;
import com.datamonitor.common.domain.model.ApplicationAsset;
import com.datamonitor.common.domain.model.ApplicationInstance;
import com.datamonitor.common.security.RequestUserContext;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {
    private final InMemoryMonitorData data;

    public ApplicationController(InMemoryMonitorData data) {
        this.data = data;
    }

    @GetMapping
    public PageResult<ApplicationAsset> list() {
        var currentUser = RequestUserContext.requireCurrent();
        data.requirePermission(currentUser, "applications:read");
        return PageResult.all(data.visibleApplications(currentUser));
    }

    @GetMapping("/{id}")
    public ApplicationAsset detail(@PathVariable("id") String id) {
        var currentUser = RequestUserContext.requireCurrent();
        data.requirePermission(currentUser, "applications:read");
        return data.requireVisibleApplication(id, currentUser);
    }

    @GetMapping("/{id}/instances")
    public List<ApplicationInstance> instances(@PathVariable("id") String id) {
        var currentUser = RequestUserContext.requireCurrent();
        data.requirePermission(currentUser, "applications:read");
        return data.visibleInstances(id, currentUser);
    }
}
