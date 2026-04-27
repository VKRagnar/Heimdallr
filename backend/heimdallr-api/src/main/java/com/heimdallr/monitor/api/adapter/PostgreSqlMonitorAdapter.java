package com.heimdallr.monitor.api.adapter;

import java.util.Map;

public interface PostgreSqlMonitorAdapter {
    Map<String, String> describeInstance(String objectId);
}
