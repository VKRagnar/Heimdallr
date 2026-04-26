package com.datamonitor.common.domain.model;

import java.util.Set;

public record DataScope(
        boolean platformAdmin,
        Set<String> applicationIds,
        Set<String> businessLines,
        Set<String> environments
) {
    public boolean canAccessApplication(ApplicationAsset application) {
        return platformAdmin
                || applicationIds.contains(application.id())
                || (businessLines.contains(application.businessLine()) && environments.contains(application.environment()));
    }

    public boolean canAccessEnvironment(String environment) {
        return platformAdmin || environments.contains(environment);
    }
}
