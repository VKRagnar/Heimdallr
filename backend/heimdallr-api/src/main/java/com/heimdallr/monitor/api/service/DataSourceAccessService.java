package com.heimdallr.monitor.api.service;

import com.heimdallr.monitor.api.dto.ApplicationAccessStatusResponse;
import com.heimdallr.monitor.api.dto.DataSourceConfigResponse;
import com.heimdallr.monitor.api.repository.MonitorData;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.AgentInstance;
import com.heimdallr.monitor.common.domain.model.ApplicationAsset;
import com.heimdallr.monitor.common.domain.model.DataSourceBinding;
import com.heimdallr.monitor.common.domain.model.DataSourceConfig;
import com.heimdallr.monitor.common.domain.model.DataSourceValidationResult;
import com.heimdallr.monitor.common.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DataSourceAccessService {
    private final MonitorData data;

    public DataSourceAccessService(MonitorData data) {
        this.data = data;
    }

    public PageResult<DataSourceConfigResponse> list(CurrentUser currentUser) {
        data.requirePermission(currentUser, "data-sources:read");
        return PageResult.all(data.visibleDataSources(currentUser).stream()
                .map(DataSourceConfigResponse::from)
                .toList());
    }

    public DataSourceConfigResponse detail(String id, CurrentUser currentUser) {
        data.requirePermission(currentUser, "data-sources:read");
        return DataSourceConfigResponse.from(data.requireVisibleDataSource(id, currentUser));
    }

    public DataSourceConfigResponse save(DataSourceConfig config, CurrentUser currentUser) {
        data.requirePermission(currentUser, "data-sources:write");
        return DataSourceConfigResponse.from(data.saveDataSource(config, currentUser));
    }

    public DataSourceValidationResult validate(String sourceId, CurrentUser currentUser) {
        data.requirePermission(currentUser, "data-sources:write");
        return data.validateDataSource(sourceId, currentUser);
    }

    public PageResult<DataSourceBinding> bindings(CurrentUser currentUser, String objectId) {
        data.requirePermission(currentUser, "data-sources:read");
        return PageResult.all(data.visibleDataSourceBindings(currentUser, objectId));
    }

    public PageResult<ApplicationAccessStatusResponse> applicationAccess(CurrentUser currentUser, String objectId) {
        data.requirePermission(currentUser, "data-sources:read");
        List<ApplicationAccessStatusResponse> statuses = data.visibleApplications(currentUser).stream()
                .map(application -> accessStatus(application, currentUser, objectId))
                .flatMap(List::stream)
                .sorted(Comparator.comparing(ApplicationAccessStatusResponse::id))
                .toList();
        return PageResult.all(statuses);
    }

    private List<ApplicationAccessStatusResponse> accessStatus(ApplicationAsset application, CurrentUser currentUser, String requestedObjectId) {
        var objects = data.visibleApplicationDependencies(application.id(), currentUser).stream()
                .filter(object -> requestedObjectId == null || object.id().equals(requestedObjectId))
                .toList();
        if (objects.isEmpty()) {
            return requestedObjectId == null ? List.of(emptyAccess(application)) : List.of();
        }
        return objects.stream().map(object -> {
            List<DataSourceBinding> bindings = data.visibleDataSourceBindings(currentUser, object.id());
            String metrics = accessSummary(bindings, "METRIC");
            String logs = accessSummary(bindings, "LOG");
            String agent = data.visibleAgents(currentUser).stream()
                    .filter(item -> object.serverIds().contains(item.serverId()))
                    .map(AgentInstance::status)
                    .min(this::agentSeverityOrder)
                    .orElse("NOT_INSTALLED");
            String status = aggregateAccess(metrics, logs, agent);
            OffsetDateTime verifiedAt = bindings.stream()
                    .map(DataSourceBinding::lastSeenAt)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            return new ApplicationAccessStatusResponse(
                    "access-" + application.id() + "-" + object.id(),
                    application.id(),
                    application.name(),
                    application.code(),
                    application.environment(),
                    application.ownerUserIds().isEmpty() ? "-" : application.ownerUserIds().get(0),
                    metrics,
                    "NOT_CONNECTED",
                    logs,
                    object.healthStatus(),
                    agent,
                    status,
                    verifiedAt
            );
        }).toList();
    }

    private ApplicationAccessStatusResponse emptyAccess(ApplicationAsset application) {
        return new ApplicationAccessStatusResponse(
                "access-" + application.id(),
                application.id(),
                application.name(),
                application.code(),
                application.environment(),
                application.ownerUserIds().isEmpty() ? "-" : application.ownerUserIds().get(0),
                "NOT_CONNECTED",
                "NOT_CONNECTED",
                "NOT_CONNECTED",
                application.accessStatus(),
                "NOT_INSTALLED",
                application.accessStatus(),
                null
        );
    }

    private String accessSummary(List<DataSourceBinding> bindings, String bindingType) {
        return bindings.stream()
                .filter(binding -> bindingType.equals(binding.bindingType()))
                .map(DataSourceBinding::accessStatus)
                .findFirst()
                .orElse("NOT_CONNECTED");
    }

    private String aggregateAccess(String metrics, String logs, String agent) {
        if ("CONNECTED".equals(metrics) && ("CONNECTED".equals(logs) || "NOT_CONNECTED".equals(logs)) && "ONLINE".equals(agent)) {
            return "CONNECTED";
        }
        if ("SOURCE_UNAVAILABLE".equals(metrics) || "NO_HEARTBEAT".equals(agent) || "CONFIG_ERROR".equals(agent)) {
            return "COLLECTOR_ERROR";
        }
        return "PARTIAL_CONNECTED";
    }

    private int agentSeverityOrder(String left, String right) {
        return Integer.compare(agentSeverity(left), agentSeverity(right));
    }

    private int agentSeverity(String status) {
        return switch (status) {
            case "NO_HEARTBEAT", "CONFIG_ERROR" -> 0;
            case "ONLINE" -> 2;
            default -> 1;
        };
    }
}
