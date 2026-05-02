package com.heimdallr.monitor.api.service;

import com.heimdallr.monitor.api.repository.MonitorData;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.AgentInstance;
import com.heimdallr.monitor.common.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class AgentAccessService {
    private final MonitorData data;

    public AgentAccessService(MonitorData data) {
        this.data = data;
    }

    public PageResult<AgentInstance> list(CurrentUser currentUser) {
        data.requirePermission(currentUser, "agents:read");
        return PageResult.all(data.visibleAgents(currentUser));
    }
}
