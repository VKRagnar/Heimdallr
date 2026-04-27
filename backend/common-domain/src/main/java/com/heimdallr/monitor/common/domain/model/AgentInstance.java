package com.heimdallr.monitor.common.domain.model;

import java.time.OffsetDateTime;

public record AgentInstance(
        String id,
        String serverId,
        String hostname,
        String environment,
        String version,
        String status,
        OffsetDateTime lastHeartbeatAt,
        String configVersion,
        String failureReason
) {
}
