package com.datamonitor.api.fixture;

import com.datamonitor.common.domain.api.ErrorCode;
import com.datamonitor.common.domain.exception.ForbiddenException;
import com.datamonitor.common.domain.exception.NotFoundException;
import com.datamonitor.common.domain.model.ApplicationAsset;
import com.datamonitor.common.domain.model.ApplicationInstance;
import com.datamonitor.common.domain.model.AuditEvent;
import com.datamonitor.common.domain.model.RoleInfo;
import com.datamonitor.common.domain.model.ServerAsset;
import com.datamonitor.common.domain.model.UserInfo;
import com.datamonitor.common.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryMonitorData {
    private final List<ApplicationAsset> applications = List.of(
            new ApplicationAsset("app-ace", "ACE", "ACE Trading", "trade", "prod", List.of("u-ace-owner"), "CONNECTED"),
            new ApplicationAsset("app-ipro", "IPRO", "iPro Portal", "trade", "staging", List.of("u-sre"), "DEGRADED"),
            new ApplicationAsset("app-cms", "CMS", "Content Management", "content", "prod", List.of("u-cms-owner"), "CONNECTED"),
            new ApplicationAsset("app-grafana", "GRAFANA", "Grafana", "core-platform", "prod", List.of("u-sre"), "CONNECTED")
    );

    private final List<ApplicationInstance> instances = List.of(
            new ApplicationInstance("inst-ace-1", "app-ace", "srv-ace-1", "ace-api-01", "prod", "ONLINE"),
            new ApplicationInstance("inst-ace-2", "app-ace", "srv-ace-2", "ace-api-02", "prod", "ONLINE"),
            new ApplicationInstance("inst-ipro-1", "app-ipro", "srv-ipro-1", "ipro-web-01", "staging", "WARN"),
            new ApplicationInstance("inst-cms-1", "app-cms", "srv-cms-1", "cms-api-01", "prod", "ONLINE"),
            new ApplicationInstance("inst-grafana-1", "app-grafana", "srv-grafana-1", "grafana-01", "prod", "ONLINE")
    );

    private final List<ServerAsset> servers = List.of(
            new ServerAsset("srv-ace-1", "ace-api-01", "10.10.1.11", "prod", Set.of("app-ace"), "CONNECTED"),
            new ServerAsset("srv-ace-2", "ace-api-02", "10.10.1.12", "prod", Set.of("app-ace"), "CONNECTED"),
            new ServerAsset("srv-ipro-1", "ipro-web-01", "10.10.2.21", "staging", Set.of("app-ipro"), "DEGRADED"),
            new ServerAsset("srv-cms-1", "cms-api-01", "10.10.3.31", "prod", Set.of("app-cms"), "CONNECTED"),
            new ServerAsset("srv-grafana-1", "grafana-01", "10.10.9.41", "prod", Set.of("app-grafana"), "CONNECTED")
    );

    private final List<AuditEvent> auditEvents = List.of(
            new AuditEvent("audit-001", "u-admin", "APPLICATION_VIEW", "APPLICATION", "app-ace", "SUCCESS", OffsetDateTime.now().minusHours(3)),
            new AuditEvent("audit-002", "u-sre", "SERVER_LIST", "SERVER", "*", "SUCCESS", OffsetDateTime.now().minusHours(2)),
            new AuditEvent("audit-003", "u-ace-owner", "ME_VIEW", "USER", "u-ace-owner", "SUCCESS", OffsetDateTime.now().minusHours(1))
    );

    private final List<RoleInfo> roles = List.of(
            new RoleInfo("r-admin", "PLATFORM_ADMIN", "平台管理员", Set.of("applications:read", "servers:read", "audit:read", "access:read")),
            new RoleInfo("r-sre", "SRE", "SRE", Set.of("applications:read", "servers:read", "audit:read")),
            new RoleInfo("r-app-owner", "APP_OWNER", "应用负责人", Set.of("applications:read", "servers:read"))
    );

    private final List<UserInfo> users = List.of(
            new UserInfo("u-admin", "platform-admin", "Platform Admin", List.of(roles.get(0)), Set.of(), Set.of("assets", "access", "audit")),
            new UserInfo("u-sre", "sre", "SRE Engineer", List.of(roles.get(1)), Set.of("core-platform"), Set.of("assets", "audit")),
            new UserInfo("u-ace-owner", "ace-owner", "ACE Owner", List.of(roles.get(2)), Set.of("trade"), Set.of("assets"))
    );

    public List<ApplicationAsset> visibleApplications(CurrentUser currentUser) {
        return applications.stream()
                .filter(currentUser.dataScope()::canAccessApplication)
                .sorted(Comparator.comparing(ApplicationAsset::id))
                .toList();
    }

    public ApplicationAsset requireVisibleApplication(String id, CurrentUser currentUser) {
        return applications.stream()
                .filter(application -> application.id().equals(id))
                .filter(currentUser.dataScope()::canAccessApplication)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Application not found"));
    }

    public List<ApplicationInstance> visibleInstances(String applicationId, CurrentUser currentUser) {
        requireVisibleApplication(applicationId, currentUser);
        return instances.stream()
                .filter(instance -> instance.applicationId().equals(applicationId))
                .sorted(Comparator.comparing(ApplicationInstance::id))
                .toList();
    }

    public List<ServerAsset> visibleServers(CurrentUser currentUser) {
        Set<String> visibleApplicationIds = visibleApplications(currentUser).stream()
                .map(ApplicationAsset::id)
                .collect(java.util.stream.Collectors.toSet());
        return servers.stream()
                .filter(server -> server.applicationIds().stream().anyMatch(visibleApplicationIds::contains))
                .filter(server -> currentUser.dataScope().canAccessEnvironment(server.environment()))
                .sorted(Comparator.comparing(ServerAsset::id))
                .toList();
    }

    public List<AuditEvent> auditEvents(CurrentUser currentUser) {
        requirePermission(currentUser, "audit:read");
        return auditEvents;
    }

    public List<UserInfo> users(CurrentUser currentUser) {
        requirePermission(currentUser, "access:read");
        return users;
    }

    public List<RoleInfo> roles(CurrentUser currentUser) {
        requirePermission(currentUser, "access:read");
        return roles;
    }

    public void requirePermission(CurrentUser currentUser, String permission) {
        if (!currentUser.hasPermission(permission)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "Missing permission: " + permission);
        }
    }
}
