package com.heimdallr.monitor.api.controller;

import com.heimdallr.monitor.api.service.AgentAccessService;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.AgentInstance;
import com.heimdallr.monitor.common.security.RequestUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {
    private final AgentAccessService service;

    public AgentController(AgentAccessService service) {
        this.service = service;
    }

    @GetMapping
    public PageResult<AgentInstance> list() {
        var currentUser = RequestUserContext.requireCurrent();
        return service.list(currentUser);
    }
}
