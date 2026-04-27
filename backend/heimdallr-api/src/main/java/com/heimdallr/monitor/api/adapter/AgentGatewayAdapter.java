package com.heimdallr.monitor.api.adapter;

import com.heimdallr.monitor.common.domain.model.AgentInstance;
import java.util.List;

public interface AgentGatewayAdapter {
    List<AgentInstance> listAgents();
}
