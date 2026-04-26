package com.heimdallr.monitor.api.controller;

import com.heimdallr.monitor.api.fixture.InMemoryMonitorData;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.AuditEvent;
import com.heimdallr.monitor.common.security.RequestUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    private final InMemoryMonitorData data;

    public SystemController(InMemoryMonitorData data) {
        this.data = data;
    }

    @GetMapping("/audit-events")
    public PageResult<AuditEvent> auditEvents() {
        return PageResult.all(data.auditEvents(RequestUserContext.requireCurrent()));
    }
}
