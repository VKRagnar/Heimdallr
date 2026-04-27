package com.heimdallr.monitor.api.adapter;

import java.util.Map;

public interface KafkaMonitorAdapter {
    Map<String, String> describeCluster(String objectId);
}
