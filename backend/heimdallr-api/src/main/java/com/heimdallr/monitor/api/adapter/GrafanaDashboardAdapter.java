package com.heimdallr.monitor.api.adapter;

import java.util.Optional;

public interface GrafanaDashboardAdapter {
    Optional<String> dashboardUrl(String objectType, String objectId);
}
