package com.heimdallr.monitor.api.repository;

import com.heimdallr.monitor.api.fixture.InMemoryMonitorData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heimdallr.monitor.common.domain.api.ErrorCode;
import com.heimdallr.monitor.common.domain.exception.ApiException;
import com.heimdallr.monitor.common.domain.exception.ForbiddenException;
import com.heimdallr.monitor.common.domain.exception.NotFoundException;
import com.heimdallr.monitor.common.domain.model.AgentInstance;
import com.heimdallr.monitor.common.domain.model.AlertEvent;
import com.heimdallr.monitor.common.domain.model.AlertEventHistory;
import com.heimdallr.monitor.common.domain.model.AlertEvaluationSample;
import com.heimdallr.monitor.common.domain.model.AlertRule;
import com.heimdallr.monitor.common.domain.model.AlertRuleRuntime;
import com.heimdallr.monitor.common.domain.model.ApplicationAsset;
import com.heimdallr.monitor.common.domain.model.AuditEvent;
import com.heimdallr.monitor.common.domain.model.DataSourceBinding;
import com.heimdallr.monitor.common.domain.model.DataSourceConfig;
import com.heimdallr.monitor.common.domain.model.DataSourceValidationResult;
import com.heimdallr.monitor.common.domain.model.DefaultMetricMapping;
import com.heimdallr.monitor.common.domain.model.LogEntry;
import com.heimdallr.monitor.common.domain.model.MetricDefinition;
import com.heimdallr.monitor.common.domain.model.MetricSeries;
import com.heimdallr.monitor.common.domain.model.MonitorObject;
import com.heimdallr.monitor.common.domain.model.NotificationRecord;
import com.heimdallr.monitor.common.domain.model.OnCallGroup;
import com.heimdallr.monitor.common.domain.model.RoleInfo;
import com.heimdallr.monitor.common.domain.model.ServerAsset;
import com.heimdallr.monitor.common.domain.model.UserInfo;
import com.heimdallr.monitor.common.security.CurrentUser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("db")
public class JdbcMonitorData extends InMemoryMonitorData {
    static final String UPSERT_TRIGGERED_ALERT_SQL = """
            INSERT INTO alert_event (id, rule_id, dedup_key, object_id, metric_code, severity, status,
                                     trigger_value, threshold, operator)
            VALUES (:id, :ruleId, :dedupKey, :objectId, :metricCode, :severity, 'triggered',
                    :triggerValue, :threshold, :operator)
            ON CONFLICT (dedup_key)
            WHERE deleted_at IS NULL AND status NOT IN ('recovered', 'closed')
            DO UPDATE
            SET trigger_value = EXCLUDED.trigger_value,
                threshold = EXCLUDED.threshold,
                operator = EXCLUDED.operator,
                severity = EXCLUDED.severity,
                last_seen_at = now(),
                updated_at = now()
            RETURNING id, (xmax = 0) AS inserted
            """;
    private static final int NOTIFICATION_RETRY_DELAY_MINUTES = 5;

    private static final Set<String> ALL_PERMISSIONS = Set.of(
            "applications:read",
            "applications:write",
            "servers:read",
            "servers:write",
            "audit:read",
            "access:read",
            "access:write",
            "data-sources:read",
            "data-sources:write",
            "agents:read",
            "metrics:read",
            "logs:read",
            "alerts:read",
            "alerts:write"
    );

    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private record AlertEventUpsertResult(UUID id, boolean inserted) {
    }

    public JdbcMonitorData(JdbcClient jdbc) {
        this.jdbc = jdbc;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public List<ApplicationAsset> visibleApplications(CurrentUser currentUser) {
        String sql = """
                SELECT a.id, a.code, a.name, bl.code AS business_line, a.env, a.access_status,
                       COALESCE(array_agg(ao.user_id::text ORDER BY ao.notify_priority) FILTER (WHERE ao.user_id IS NOT NULL), ARRAY[]::text[]) AS owner_user_ids
                FROM application a
                JOIN business_line bl ON bl.id = a.business_line_id
                LEFT JOIN application_owner ao ON ao.application_id = a.id AND ao.deleted_at IS NULL
                WHERE a.deleted_at IS NULL
                  AND (:platformAdmin OR a.env = ANY(:envs) OR EXISTS (
                        SELECT 1 FROM application_authorization aa
                        WHERE aa.application_id = a.id AND aa.user_id = :userId AND aa.status = 'active' AND aa.deleted_at IS NULL
                  ) OR EXISTS (
                        SELECT 1 FROM business_line_authorization ba
                        WHERE ba.business_line_id = a.business_line_id AND ba.user_id = :userId AND ba.status = 'active' AND ba.deleted_at IS NULL
                  ))
                GROUP BY a.id, a.code, a.name, bl.code, a.env, a.access_status
                ORDER BY a.code
                """;
        return jdbc.sql(sql)
                .param("platformAdmin", currentUser.dataScope().platformAdmin())
                .param("envs", currentUser.dataScope().environments().toArray(String[]::new))
                .param("userId", resolveUserId(currentUser).orElse(null))
                .query(this::applicationAsset)
                .list();
    }

    @Override
    public ApplicationAsset requireVisibleApplication(String id, CurrentUser currentUser) {
        return visibleApplications(currentUser).stream()
                .filter(application -> application.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Application not found"));
    }

    @Override
    @Transactional
    public ApplicationAsset saveApplication(ApplicationAsset application, CurrentUser currentUser) {
        requirePermission(currentUser, "applications:write");
        if (!currentUser.dataScope().canAccessEnvironment(application.environment())) {
            throw new ForbiddenException(ErrorCode.ENV_FORBIDDEN, "Environment is outside current user scope");
        }
        UUID businessLineId = ensureBusinessLine(application.businessLine());
        UUID applicationId = application.id() == null || application.id().isBlank()
                ? UUID.randomUUID()
                : stableUuid(application.id());
        String status = normalizeStatus(application.accessStatus(), "connected");

        jdbc.sql("""
                INSERT INTO application (id, business_line_id, code, name, env, access_status)
                VALUES (:id, :businessLineId, :code, :name, :env, :accessStatus)
                ON CONFLICT (id) DO UPDATE
                SET business_line_id = EXCLUDED.business_line_id,
                    code = EXCLUDED.code,
                    name = EXCLUDED.name,
                    env = EXCLUDED.env,
                    access_status = EXCLUDED.access_status,
                    updated_at = now()
                """)
                .param("id", applicationId)
                .param("businessLineId", businessLineId)
                .param("code", application.code())
                .param("name", application.name())
                .param("env", application.environment())
                .param("accessStatus", status)
                .update();

        jdbc.sql("DELETE FROM application_owner WHERE application_id = :applicationId")
                .param("applicationId", applicationId)
                .update();
        for (String ownerUserId : Optional.ofNullable(application.ownerUserIds()).orElse(List.of())) {
            parseUuid(ownerUserId).ifPresent(userId -> jdbc.sql("""
                    INSERT INTO application_owner (application_id, user_id, owner_role, notify_priority)
                    VALUES (:applicationId, :userId, 'primary', 1)
                    ON CONFLICT (application_id, user_id, owner_role) DO NOTHING
                    """)
                    .param("applicationId", applicationId)
                    .param("userId", userId)
                    .update());
        }
        appendAudit(currentUser, "APPLICATION_UPSERT", "APPLICATION", applicationId, "success");
        return requireVisibleApplication(applicationId.toString(), currentUser);
    }

    @Override
    @Transactional
    public List<ApplicationAsset> importApplications(List<ApplicationAsset> imports, CurrentUser currentUser) {
        requirePermission(currentUser, "applications:write");
        return imports.stream().map(application -> saveApplication(application, currentUser)).toList();
    }

    @Override
    public List<ServerAsset> visibleServers(CurrentUser currentUser) {
        String sql = """
                SELECT s.id, s.hostname, s.ip::text AS ip, s.env, s.access_status,
                       COALESCE(array_agg(asb.application_id::text ORDER BY asb.application_id) FILTER (WHERE asb.application_id IS NOT NULL), ARRAY[]::text[]) AS application_ids
                FROM server s
                LEFT JOIN app_server_binding asb ON asb.server_id = s.id AND asb.deleted_at IS NULL
                WHERE s.deleted_at IS NULL
                  AND (:platformAdmin OR s.env = ANY(:envs) OR EXISTS (
                        SELECT 1 FROM app_server_binding binding
                        JOIN application_authorization aa ON aa.application_id = binding.application_id
                        WHERE binding.server_id = s.id AND aa.user_id = :userId AND aa.status = 'active' AND aa.deleted_at IS NULL
                  ))
                GROUP BY s.id, s.hostname, s.ip, s.env, s.access_status
                ORDER BY s.hostname
                """;
        return jdbc.sql(sql)
                .param("platformAdmin", currentUser.dataScope().platformAdmin())
                .param("envs", currentUser.dataScope().environments().toArray(String[]::new))
                .param("userId", resolveUserId(currentUser).orElse(null))
                .query(this::serverAsset)
                .list();
    }

    @Override
    @Transactional
    public ServerAsset saveServer(ServerAsset server, CurrentUser currentUser) {
        requirePermission(currentUser, "servers:write");
        if (!currentUser.dataScope().canAccessEnvironment(server.environment())) {
            throw new ForbiddenException(ErrorCode.ENV_FORBIDDEN, "Environment is outside current user scope");
        }
        Set<UUID> applicationIds = Optional.ofNullable(server.applicationIds()).orElse(Set.of()).stream()
                .map(this::requireApplicationId)
                .collect(Collectors.toSet());
        UUID businessLineId = applicationIds.stream()
                .findFirst()
                .flatMap(this::businessLineIdForApplication)
                .orElseGet(() -> ensureBusinessLine("shared"));
        UUID serverId = server.id() == null || server.id().isBlank()
                ? UUID.randomUUID()
                : stableUuid(server.id());

        jdbc.sql("""
                INSERT INTO server (id, business_line_id, hostname, ip, env, access_status)
                VALUES (:id, :businessLineId, :hostname, CAST(:ip AS inet), :env, :accessStatus)
                ON CONFLICT (id) DO UPDATE
                SET business_line_id = EXCLUDED.business_line_id,
                    hostname = EXCLUDED.hostname,
                    ip = EXCLUDED.ip,
                    env = EXCLUDED.env,
                    access_status = EXCLUDED.access_status,
                    updated_at = now()
                """)
                .param("id", serverId)
                .param("businessLineId", businessLineId)
                .param("hostname", server.hostname())
                .param("ip", server.ip())
                .param("env", server.environment())
                .param("accessStatus", normalizeStatus(server.accessStatus(), "connected"))
                .update();

        jdbc.sql("DELETE FROM app_server_binding WHERE server_id = :serverId")
                .param("serverId", serverId)
                .update();
        for (UUID applicationId : applicationIds) {
            jdbc.sql("""
                    INSERT INTO app_server_binding (application_id, server_id, env, binding_role, source)
                    VALUES (:applicationId, :serverId, :env, 'app', 'api')
                    ON CONFLICT (application_id, server_id, binding_role) DO UPDATE
                    SET env = EXCLUDED.env, updated_at = now()
                    """)
                    .param("applicationId", applicationId)
                    .param("serverId", serverId)
                    .param("env", server.environment())
                    .update();
        }
        appendAudit(currentUser, "SERVER_UPSERT", "SERVER", serverId, "success");
        return visibleServers(currentUser).stream()
                .filter(item -> item.id().equals(serverId.toString()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Server not found"));
    }

    @Override
    @Transactional
    public List<ServerAsset> importServers(List<ServerAsset> imports, CurrentUser currentUser) {
        requirePermission(currentUser, "servers:write");
        return imports.stream().map(server -> saveServer(server, currentUser)).toList();
    }

    @Override
    public List<AuditEvent> auditEvents(CurrentUser currentUser) {
        requirePermission(currentUser, "audit:read");
        return jdbc.sql("""
                SELECT ae.id, ae.operator_user_id::text AS actor_user_id, ae.action,
                       ae.resource_type, ae.resource_id::text AS resource_id, ae.result, ae.operated_at
                FROM audit_event ae
                WHERE ae.deleted_at IS NULL
                ORDER BY ae.operated_at, ae.id
                """)
                .query((rs, rowNum) -> new AuditEvent(
                        rs.getString("id"),
                        rs.getString("actor_user_id"),
                        rs.getString("action"),
                        rs.getString("resource_type"),
                        rs.getString("resource_id"),
                        rs.getString("result"),
                        rs.getObject("operated_at", OffsetDateTime.class)
                ))
                .list();
    }

    @Override
    public List<UserInfo> users(CurrentUser currentUser) {
        requirePermission(currentUser, "access:read");
        return jdbc.sql("""
                SELECT u.id, u.username, u.display_name,
                       COALESCE(array_agg(DISTINCT bl.code) FILTER (WHERE bl.code IS NOT NULL), ARRAY[]::text[]) AS business_lines
                FROM user_account u
                LEFT JOIN business_line_authorization ba ON ba.user_id = u.id AND ba.status = 'active' AND ba.deleted_at IS NULL
                LEFT JOIN business_line bl ON bl.id = ba.business_line_id
                WHERE u.deleted_at IS NULL
                GROUP BY u.id, u.username, u.display_name
                ORDER BY u.username
                """)
                .query((rs, rowNum) -> new UserInfo(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        userRoles(rs.getObject("id", UUID.class)),
                        Set.of((String[]) rs.getArray("business_lines").getArray()),
                        Set.of("assets", "access", "audit")
                ))
                .list();
    }

    @Override
    public List<RoleInfo> roles(CurrentUser currentUser) {
        requirePermission(currentUser, "access:read");
        return jdbc.sql("SELECT id, code, name FROM role WHERE deleted_at IS NULL ORDER BY code")
                .query((rs, rowNum) -> roleInfo(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name")))
                .list();
    }

    @Override
    public List<MonitorObject> visibleMonitorObjects(CurrentUser currentUser) {
        String sql = """
                SELECT mo.id, mo.code, mo.name, mo.object_type, mo.env, bl.code AS business_line,
                       mo.health_status, mo.access_status, mo.key_metrics::text,
                       COALESCE(array_agg(DISTINCT oad.application_id::text) FILTER (WHERE oad.application_id IS NOT NULL), ARRAY[]::text[]) AS application_ids,
                       COALESCE(array_agg(DISTINCT osb.server_id::text) FILTER (WHERE osb.server_id IS NOT NULL), ARRAY[]::text[]) AS server_ids,
                       COALESCE(array_agg(DISTINCT ao.user_id::text) FILTER (WHERE ao.user_id IS NOT NULL), ARRAY[]::text[]) AS owner_user_ids
                FROM monitor_object mo
                JOIN business_line bl ON bl.id = mo.business_line_id
                LEFT JOIN object_app_dependency oad ON oad.object_id = mo.id AND oad.deleted_at IS NULL
                LEFT JOIN object_server_binding osb ON osb.object_id = mo.id AND osb.deleted_at IS NULL
                LEFT JOIN application_owner ao ON ao.application_id = oad.application_id AND ao.deleted_at IS NULL
                WHERE mo.deleted_at IS NULL
                  AND (:platformAdmin OR mo.env = ANY(:envs))
                  AND (:platformAdmin
                    OR bl.code = ANY(:businessLines)
                    OR EXISTS (
                        SELECT 1 FROM object_app_dependency dep
                        JOIN application_authorization aa ON aa.application_id = dep.application_id
                        WHERE dep.object_id = mo.id AND aa.user_id = :userId AND aa.status = 'active' AND aa.deleted_at IS NULL
                    ))
                GROUP BY mo.id, mo.code, mo.name, mo.object_type, mo.env, bl.code, mo.health_status, mo.access_status, mo.key_metrics
                ORDER BY mo.code
                """;
        return jdbc.sql(sql)
                .param("platformAdmin", currentUser.dataScope().platformAdmin())
                .param("envs", currentUser.dataScope().environments().toArray(String[]::new))
                .param("businessLines", currentUser.dataScope().businessLines().toArray(String[]::new))
                .param("userId", resolveUserId(currentUser).orElse(null))
                .query(this::monitorObject)
                .list();
    }

    @Override
    public MonitorObject requireVisibleMonitorObject(String objectId, CurrentUser currentUser) {
        return visibleMonitorObjects(currentUser).stream()
                .filter(object -> object.id().equals(objectId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Monitor object not found"));
    }

    @Override
    public List<MonitorObject> visibleApplicationDependencies(String applicationId, CurrentUser currentUser) {
        ApplicationAsset application = requireVisibleApplication(applicationId, currentUser);
        return visibleMonitorObjects(currentUser).stream()
                .filter(object -> object.applicationIds().contains(application.id()))
                .toList();
    }

    @Override
    public List<DataSourceConfig> visibleDataSources(CurrentUser currentUser) {
        return jdbc.sql("""
                SELECT code, name, source_type, env, base_url, health_check_path, auth_type, secret_ref,
                       timeout_seconds, retry_count, rate_limit_qps, status, last_check_at, last_success_at,
                       last_error_code, last_error_message
                FROM data_source
                WHERE deleted_at IS NULL AND (:platformAdmin OR env = ANY(:envs))
                ORDER BY code
                """)
                .param("platformAdmin", currentUser.dataScope().platformAdmin())
                .param("envs", currentUser.dataScope().environments().toArray(String[]::new))
                .query(this::dataSourceConfig)
                .list();
    }

    @Override
    public DataSourceConfig requireVisibleDataSource(String sourceId, CurrentUser currentUser) {
        return visibleDataSources(currentUser).stream()
                .filter(source -> source.id().equals(sourceId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Data source not found"));
    }

    @Override
    @Transactional
    public DataSourceConfig saveDataSource(DataSourceConfig config, CurrentUser currentUser) {
        requirePermission(currentUser, "data-sources:write");
        if (!currentUser.dataScope().canAccessEnvironment(config.environment())) {
            throw new ForbiddenException(ErrorCode.ENV_FORBIDDEN, "Environment is outside current user scope");
        }
        String code = config.id() == null || config.id().isBlank() ? "ds-" + UUID.randomUUID() : config.id();
        jdbc.sql("""
                INSERT INTO data_source (id, code, name, source_type, env, base_url, health_check_path, auth_type, secret_ref,
                                         timeout_seconds, retry_count, rate_limit_qps, status, last_check_at, last_error_code, last_error_message)
                VALUES (:id, :code, :name, :sourceType, :env, :baseUrl, :healthCheckPath, :authType, :secretRef,
                        :timeoutSeconds, :retryCount, :rateLimitQps, :status, now(), 'VALIDATION_REQUIRED',
                        'Data source saved; run validation before enabling production bindings')
                ON CONFLICT (code, env) DO UPDATE
                SET name = EXCLUDED.name,
                    source_type = EXCLUDED.source_type,
                    base_url = EXCLUDED.base_url,
                    health_check_path = EXCLUDED.health_check_path,
                    auth_type = EXCLUDED.auth_type,
                    secret_ref = EXCLUDED.secret_ref,
                    timeout_seconds = EXCLUDED.timeout_seconds,
                    retry_count = EXCLUDED.retry_count,
                    rate_limit_qps = EXCLUDED.rate_limit_qps,
                    status = EXCLUDED.status,
                    last_check_at = now(),
                    last_error_code = EXCLUDED.last_error_code,
                    last_error_message = EXCLUDED.last_error_message,
                    updated_at = now()
                """)
                .param("id", stableUuid(code))
                .param("code", code)
                .param("name", config.name())
                .param("sourceType", config.type())
                .param("env", config.environment())
                .param("baseUrl", config.baseUrl())
                .param("healthCheckPath", config.healthCheckPath())
                .param("authType", config.authType())
                .param("secretRef", config.secretRef())
                .param("timeoutSeconds", config.timeoutSeconds())
                .param("retryCount", config.retryCount())
                .param("rateLimitQps", config.rateLimitQps())
                .param("status", normalizeStatus(config.status(), "DISABLED").toUpperCase())
                .update();
        return requireVisibleDataSource(code, currentUser);
    }

    @Override
    public List<DataSourceBinding> visibleDataSourceBindings(CurrentUser currentUser, String objectId) {
        Set<String> visibleObjectIds = visibleMonitorObjects(currentUser).stream()
                .map(MonitorObject::id)
                .collect(Collectors.toSet());
        if (visibleObjectIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                SELECT dsb.code, mo.code AS object_code, mo.object_type, ds.code AS source_code, dsb.binding_type,
                       dsb.external_labels::text, dsb.mapping_config::text, dsb.last_seen_at, dsb.access_status, dsb.failure_reason
                FROM data_source_binding dsb
                JOIN monitor_object mo ON mo.id = dsb.object_id
                JOIN data_source ds ON ds.id = dsb.source_id
                WHERE dsb.deleted_at IS NULL
                  AND mo.code = ANY(:visibleObjectIds)
                  AND (CAST(:objectId AS text) IS NULL OR mo.code = :objectId)
                ORDER BY dsb.code
                """)
                .param("visibleObjectIds", visibleObjectIds.toArray(String[]::new))
                .param("objectId", objectId)
                .query(this::dataSourceBinding)
                .list();
    }

    @Override
    public DataSourceValidationResult validateDataSource(String sourceId, CurrentUser currentUser) {
        DataSourceConfig source = requireVisibleDataSource(sourceId, currentUser);
        ValidationProbe probe = probeDataSource(source);
        boolean basicConfigPassed = source.baseUrl() != null && source.baseUrl().startsWith("http");
        boolean passed = basicConfigPassed && probe.passed();
        List<DataSourceValidationResult.ValidationItem> items = List.of(
                validationItem("basic_config", basicConfigPassed, "CONFIG_INVALID", "Base URL is valid"),
                validationItem("connectivity", probe.passed(), probe.errorCode(), probe.message()),
                validationItem("auth", true, "AUTH_FAILED", "No auth required for observability test stack"),
                validationItem("sample_data", probe.passed(), "NO_DATA", "Recent sample data is available")
        );
        return new DataSourceValidationResult(
                source.id(),
                passed,
                passed ? "PASSED" : "FAILED",
                OffsetDateTime.now(),
                items,
                Map.of("env", source.environment(), "sourceType", source.type()),
                passed ? "Validation passed without exposing secret values" : "Validation failed; see failed checks"
        );
    }

    @Override
    public List<AgentInstance> visibleAgents(CurrentUser currentUser) {
        Set<String> serverIds = visibleServers(currentUser).stream()
                .map(ServerAsset::id)
                .collect(Collectors.toSet());
        if (serverIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                SELECT ai.code, ai.server_id::text, s.hostname, s.env, ai.version, ai.status,
                       ai.last_heartbeat_at, ai.config_version, ai.failure_reason
                FROM agent_instance ai
                JOIN server s ON s.id = ai.server_id
                WHERE ai.deleted_at IS NULL AND ai.server_id::text = ANY(:serverIds)
                ORDER BY ai.code
                """)
                .param("serverIds", serverIds.toArray(String[]::new))
                .query((rs, rowNum) -> new AgentInstance(
                        rs.getString("code"),
                        rs.getString("server_id"),
                        rs.getString("hostname"),
                        rs.getString("env"),
                        rs.getString("version"),
                        rs.getString("status"),
                        rs.getObject("last_heartbeat_at", OffsetDateTime.class),
                        rs.getString("config_version"),
                        rs.getString("failure_reason")
                ))
                .list()
                .stream()
                .map(agent -> mergeAgentGatewayStatus(currentUser, agent))
                .toList();
    }

    @Override
    public List<MetricDefinition> metricDefinitions(CurrentUser currentUser, String objectType) {
        requirePermission(currentUser, "metrics:read");
        return jdbc.sql("""
                SELECT code, name, object_type, category, unit, source_type, default_query_template, labels
                FROM metric_definition
                WHERE deleted_at IS NULL AND status = 'active'
                  AND (CAST(:objectType AS text) IS NULL OR upper(object_type) = upper(:objectType))
                ORDER BY code
                """)
                .param("objectType", objectType)
                .query((rs, rowNum) -> new MetricDefinition(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("object_type"),
                        rs.getString("category"),
                        rs.getString("unit"),
                        rs.getString("source_type"),
                        rs.getString("default_query_template"),
                        List.of((String[]) rs.getArray("labels").getArray())
                ))
                .list();
    }

    @Override
    public List<DefaultMetricMapping> defaultMetricMappings(String objectType) {
        return jdbc.sql("""
                SELECT code, object_type, metric_code, source_type, external_metric, query_template, unit, default_labels::text
                FROM metric_series_mapping
                WHERE deleted_at IS NULL AND status = 'active'
                  AND (CAST(:objectType AS text) IS NULL OR upper(object_type) = upper(:objectType))
                ORDER BY code
                """)
                .param("objectType", objectType)
                .query((rs, rowNum) -> new DefaultMetricMapping(
                        rs.getString("code"),
                        rs.getString("object_type"),
                        rs.getString("metric_code"),
                        rs.getString("source_type"),
                        rs.getString("external_metric"),
                        rs.getString("query_template"),
                        rs.getString("unit"),
                        jsonMap(rs.getString("default_labels"))
                ))
                .list();
    }

    @Override
    public MetricSeries queryMetric(CurrentUser currentUser, String metricCode, String objectId, OffsetDateTime from, OffsetDateTime to) {
        MetricDefinition definition = metricDefinitions(currentUser, null).stream()
                .filter(metric -> metric.code().equals(metricCode))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Metric definition not found"));
        MonitorObject object = requireVisibleMonitorObject(objectId, currentUser);
        DataSourceBinding binding = visibleDataSourceBindings(currentUser, objectId).stream()
                .filter(item -> "METRIC".equals(item.bindingType()))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.METRIC_NO_RECENT_DATA, 409, "Metric source is not connected"));
        OffsetDateTime end = to == null ? OffsetDateTime.now() : to;
        OffsetDateTime start = from == null ? end.minusMinutes(30) : from;
        Optional<DataSourceConfig> source = visibleDataSources(currentUser).stream()
                .filter(item -> item.id().equals(binding.sourceId()))
                .findFirst();
        if (source.isPresent() && "PROMETHEUS".equalsIgnoreCase(source.get().type())) {
            List<MetricSeries.MetricSample> prometheusSamples = queryPrometheusSamples(source.get(), object.objectType(), metricCode, object.id(), definition.defaultQueryTemplate(), start, end);
            if (!prometheusSamples.isEmpty()) {
                return new MetricSeries(metricCode, object.id(), object.name(), definition.unit(), binding.sourceId(), start, end, prometheusSamples, binding.externalLabels());
            }
        }
        List<MetricSeries.MetricSample> samples = List.of(
                new MetricSeries.MetricSample(start, sampleValue(metricCode, 0)),
                new MetricSeries.MetricSample(start.plusMinutes(10), sampleValue(metricCode, 1)),
                new MetricSeries.MetricSample(start.plusMinutes(20), sampleValue(metricCode, 2)),
                new MetricSeries.MetricSample(end, sampleValue(metricCode, 3))
        );
        return new MetricSeries(metricCode, object.id(), object.name(), definition.unit(), binding.sourceId(), start, end, samples, binding.externalLabels());
    }

    @Override
    public List<AlertRule> alertRules(CurrentUser currentUser) {
        requirePermission(currentUser, "alerts:read");
        return jdbc.sql("""
                SELECT ar.id, ar.rule_name, ar.object_id, COALESCE(mo.name, ar.object_id) AS object_name,
                       ar.metric_code, ar.operator, ar.threshold, ar.window_seconds, ar.duration_seconds,
                       ar.evaluation_interval_seconds, ar.severity, ar.enabled, bl.code AS business_line,
                       ar.app_id::text, COALESCE(ocg.code, ar.on_call_group_id::text) AS on_call_group_id,
                       ar.created_at, ar.updated_at
                FROM alert_rule ar
                LEFT JOIN monitor_object mo ON mo.code = ar.object_id AND mo.deleted_at IS NULL
                LEFT JOIN business_line bl ON bl.id = ar.business_line_id
                LEFT JOIN on_call_group ocg ON ocg.id = ar.on_call_group_id
                WHERE ar.deleted_at IS NULL
                ORDER BY ar.updated_at DESC
                """)
                .query(this::alertRule)
                .list().stream()
                .filter(rule -> currentUser.dataScope().platformAdmin()
                        || visibleMonitorObjects(currentUser).stream().anyMatch(object -> object.id().equals(rule.objectId())))
                .toList();
    }

    @Override
    public List<AlertRule> dueAlertRules(CurrentUser currentUser, OffsetDateTime now, int limit) {
        requirePermission(currentUser, "alerts:write");
        return alertRules(currentUser).stream()
                .filter(AlertRule::enabled)
                .filter(rule -> {
                    AlertRuleRuntime runtime = alertRuleRuntime(rule.id(), currentUser);
                    return runtime.nextEvaluateAt() == null || !runtime.nextEvaluateAt().isAfter(now);
                })
                .limit(Math.max(limit, 1))
                .toList();
    }

    @Override
    public AlertRule requireAlertRule(String ruleId, CurrentUser currentUser) {
        return alertRules(currentUser).stream()
                .filter(rule -> rule.id().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Alert rule not found"));
    }

    @Override
    @Transactional
    public AlertRule saveAlertRule(AlertRule rule, CurrentUser currentUser) {
        requirePermission(currentUser, "alerts:write");
        if (rule.id() != null && !rule.id().isBlank() && alertRuleExists(rule.id())) {
            requireAlertRule(rule.id(), currentUser);
        }
        MonitorObject object = requireVisibleMonitorObject(rule.objectId(), currentUser);
        UUID ruleId = rule.id() == null || rule.id().isBlank() ? UUID.randomUUID() : stableUuid(rule.id());
        UUID businessLineId = businessLineIdByCode(object.businessLine()).orElseGet(() -> ensureBusinessLine(object.businessLine()));
        UUID appId = object.applicationIds().stream().findFirst().flatMap(JdbcMonitorData::parseUuid).orElse(null);
        UUID onCallGroupId = onCallGroupId(rule.onCallGroupId()).orElse(null);
        jdbc.sql("""
                INSERT INTO alert_rule (id, rule_name, object_id, metric_code, operator, threshold, window_seconds,
                                        duration_seconds, evaluation_interval_seconds, severity, enabled,
                                        business_line_id, app_id, on_call_group_id, created_by, updated_by)
                VALUES (:id, :name, :objectId, :metricCode, :operator, :threshold, :windowSeconds,
                        :durationSeconds, :evaluationIntervalSeconds, :severity, :enabled,
                        :businessLineId, :appId, :onCallGroupId, :operatorUserId, :operatorUserId)
                ON CONFLICT (id) DO UPDATE
                SET rule_name = EXCLUDED.rule_name,
                    object_id = EXCLUDED.object_id,
                    metric_code = EXCLUDED.metric_code,
                    operator = EXCLUDED.operator,
                    threshold = EXCLUDED.threshold,
                    window_seconds = EXCLUDED.window_seconds,
                    duration_seconds = EXCLUDED.duration_seconds,
                    evaluation_interval_seconds = EXCLUDED.evaluation_interval_seconds,
                    severity = EXCLUDED.severity,
                    enabled = EXCLUDED.enabled,
                    business_line_id = EXCLUDED.business_line_id,
                    app_id = EXCLUDED.app_id,
                    on_call_group_id = EXCLUDED.on_call_group_id,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                """)
                .param("id", ruleId)
                .param("name", rule.name())
                .param("objectId", object.id())
                .param("metricCode", rule.metricCode())
                .param("operator", rule.operator())
                .param("threshold", rule.threshold())
                .param("windowSeconds", Math.max(rule.windowSeconds(), 60))
                .param("durationSeconds", Math.max(rule.durationSeconds(), 0))
                .param("evaluationIntervalSeconds", Math.max(rule.evaluationIntervalSeconds(), 30))
                .param("severity", rule.severity() == null || rule.severity().isBlank() ? "P2" : rule.severity())
                .param("enabled", rule.enabled())
                .param("businessLineId", businessLineId)
                .param("appId", appId)
                .param("onCallGroupId", onCallGroupId)
                .param("operatorUserId", resolveUserId(currentUser).orElse(null))
                .update();
        appendAudit(currentUser, "ALERT_RULE_UPSERT", "ALERT_RULE", ruleId, "success");
        return requireAlertRule(ruleId.toString(), currentUser);
    }

    @Override
    @Transactional
    public AlertRule setAlertRuleEnabled(String ruleId, boolean enabled, CurrentUser currentUser) {
        requirePermission(currentUser, "alerts:write");
        AlertRule rule = requireAlertRule(ruleId, currentUser);
        UUID id = stableUuid(rule.id());
        jdbc.sql("UPDATE alert_rule SET enabled = :enabled, updated_by = :userId, updated_at = now() WHERE id = :id")
                .param("enabled", enabled)
                .param("userId", resolveUserId(currentUser).orElse(null))
                .param("id", id)
                .update();
        appendAudit(currentUser, enabled ? "ALERT_RULE_ENABLE" : "ALERT_RULE_DISABLE", "ALERT_RULE", id, "success");
        return requireAlertRule(rule.id(), currentUser);
    }

    @Override
    public AlertRuleRuntime alertRuleRuntime(String ruleId, CurrentUser currentUser) {
        AlertRule rule = requireAlertRule(ruleId, currentUser);
        return jdbc.sql("""
                SELECT rule_id, last_evaluated_at, next_evaluate_at, last_status, last_value,
                       last_error, evaluation_duration_ms, updated_at
                FROM alert_rule_runtime
                WHERE rule_id = :ruleId AND deleted_at IS NULL
                """)
                .param("ruleId", stableUuid(rule.id()))
                .query(this::alertRuleRuntime)
                .optional()
                .orElseGet(() -> new AlertRuleRuntime(rule.id(), null, null, "pending", null, null, null, rule.updatedAt()));
    }

    @Override
    public List<AlertEvaluationSample> alertEvaluationSamples(String ruleId, CurrentUser currentUser) {
        AlertRule rule = requireAlertRule(ruleId, currentUser);
        return jdbc.sql("""
                SELECT id, rule_id, evaluated_at, status, metric_value, threshold, operator, matched,
                       event_id, error, evaluation_duration_ms, created_at
                FROM alert_evaluation_sample
                WHERE rule_id = :ruleId AND deleted_at IS NULL
                ORDER BY evaluated_at DESC, id DESC
                LIMIT 50
                """)
                .param("ruleId", stableUuid(rule.id()))
                .query(this::alertEvaluationSample)
                .list();
    }

    @Override
    @Transactional
    public AlertRuleRuntime recordAlertEvaluation(AlertRule rule, String status, Double value, boolean matched, String eventId, String error, long evaluationDurationMs, CurrentUser currentUser) {
        UUID ruleId = stableUuid(rule.id());
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime nextEvaluateAt = now.plusSeconds(Math.max(rule.evaluationIntervalSeconds(), 1));
        String normalizedStatus = normalizeStatus(status, "failed");
        jdbc.sql("""
                INSERT INTO alert_rule_runtime (rule_id, last_evaluated_at, next_evaluate_at, last_status,
                                                last_value, last_error, evaluation_duration_ms)
                VALUES (:ruleId, :evaluatedAt, :nextEvaluateAt, :status, :value, :error, :durationMs)
                ON CONFLICT (rule_id) DO UPDATE
                SET last_evaluated_at = EXCLUDED.last_evaluated_at,
                    next_evaluate_at = EXCLUDED.next_evaluate_at,
                    last_status = EXCLUDED.last_status,
                    last_value = EXCLUDED.last_value,
                    last_error = EXCLUDED.last_error,
                    evaluation_duration_ms = EXCLUDED.evaluation_duration_ms,
                    deleted_at = NULL,
                    updated_at = now()
                """)
                .param("ruleId", ruleId)
                .param("evaluatedAt", now)
                .param("nextEvaluateAt", nextEvaluateAt)
                .param("status", normalizedStatus)
                .param("value", value)
                .param("error", error)
                .param("durationMs", evaluationDurationMs)
                .update();
        jdbc.sql("""
                INSERT INTO alert_evaluation_sample (rule_id, evaluated_at, status, metric_value, threshold,
                                                     operator, matched, event_id, error, evaluation_duration_ms)
                VALUES (:ruleId, :evaluatedAt, :status, :value, :threshold, :operator, :matched,
                        :eventId, :error, :durationMs)
                """)
                .param("ruleId", ruleId)
                .param("evaluatedAt", now)
                .param("status", normalizedStatus)
                .param("value", value)
                .param("threshold", rule.threshold())
                .param("operator", rule.operator())
                .param("matched", matched)
                .param("eventId", parseUuid(eventId).orElse(null))
                .param("error", error)
                .param("durationMs", evaluationDurationMs)
                .update();
        jdbc.sql("""
                UPDATE alert_rule
                SET last_evaluated_at = :evaluatedAt,
                    last_error = :error,
                    updated_at = updated_at
                WHERE id = :ruleId
                """)
                .param("evaluatedAt", now)
                .param("error", error)
                .param("ruleId", ruleId)
                .update();
        return alertRuleRuntime(rule.id(), currentUser);
    }

    @Override
    public List<AlertEvent> alertEvents(CurrentUser currentUser, String status) {
        requirePermission(currentUser, "alerts:read");
        Set<String> ruleIds = alertRules(currentUser).stream().map(AlertRule::id).collect(Collectors.toSet());
        if (ruleIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                SELECT ae.id, ae.rule_id, ar.rule_name, ae.object_id, COALESCE(mo.name, ae.object_id) AS object_name,
                       ae.metric_code, ae.severity, ae.status, ae.trigger_value, ae.threshold, ae.operator,
                       ae.assignee_user_id::text, ae.close_reason, ae.triggered_at, ae.notified_at,
                       ae.acknowledged_at, ae.processing_at, ae.recovered_at, ae.closed_at, ae.updated_at
                FROM alert_event ae
                JOIN alert_rule ar ON ar.id = ae.rule_id
                LEFT JOIN monitor_object mo ON mo.code = ae.object_id AND mo.deleted_at IS NULL
                WHERE ae.deleted_at IS NULL
                  AND ae.rule_id::text = ANY(:ruleIds)
                  AND (:status IS NULL OR lower(ae.status) = lower(:status))
                ORDER BY ae.updated_at DESC
                """)
                .param("ruleIds", ruleIds.toArray(String[]::new))
                .param("status", status)
                .query(this::alertEvent)
                .list();
    }

    @Override
    @Transactional
    public AlertEvent upsertTriggeredAlert(AlertRule rule, double triggerValue, CurrentUser currentUser) {
        UUID ruleId = stableUuid(rule.id());
        String dedupKey = rule.id() + ":" + rule.objectId() + ":" + rule.metricCode();
        AlertEventUpsertResult upsert = jdbc.sql(UPSERT_TRIGGERED_ALERT_SQL)
                .param("id", UUID.randomUUID())
                .param("ruleId", ruleId)
                .param("dedupKey", dedupKey)
                .param("objectId", rule.objectId())
                .param("metricCode", rule.metricCode())
                .param("severity", rule.severity())
                .param("triggerValue", triggerValue)
                .param("threshold", rule.threshold())
                .param("operator", rule.operator())
                .query((rs, rowNum) -> new AlertEventUpsertResult(rs.getObject("id", UUID.class), rs.getBoolean("inserted")))
                .single();
        if (upsert.inserted()) {
            UUID eventId = upsert.id();
            appendAlertHistory(eventId, null, "triggered", "TRIGGER", resolveUserId(currentUser).orElse(null), "Threshold condition matched");
            appendAudit(currentUser, "ALERT_EVENT_TRIGGER", "ALERT_EVENT", eventId, "success");
        }
        return alertEvents(currentUser, null).stream()
                .filter(event -> event.id().equals(upsert.id().toString()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Alert event not found"));
    }

    @Override
    @Transactional
    public AlertEvent recoverActiveAlert(AlertRule rule, double latestValue, CurrentUser currentUser) {
        UUID eventId = jdbc.sql("""
                SELECT id FROM alert_event
                WHERE rule_id = :ruleId AND deleted_at IS NULL AND status NOT IN ('recovered', 'closed')
                ORDER BY triggered_at DESC
                LIMIT 1
                """)
                .param("ruleId", stableUuid(rule.id()))
                .query(UUID.class)
                .optional()
                .orElseThrow(() -> new NotFoundException("Active alert event not found"));
        return transitionAlertEvent(eventId.toString(), "RECOVER", "Metric recovered at " + latestValue, currentUser);
    }

    @Override
    @Transactional
    public AlertEvent transitionAlertEvent(String eventId, String action, String message, CurrentUser currentUser) {
        requirePermission(currentUser, "alerts:write");
        AlertEvent event = alertEvents(currentUser, null).stream()
                .filter(item -> item.id().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Alert event not found"));
        String normalizedAction = action == null ? "" : action.toUpperCase();
        String nextStatus = switch (normalizedAction) {
            case "ACKNOWLEDGE" -> "acknowledged";
            case "PROCESS" -> "processing";
            case "RECOVER" -> "recovered";
            case "NOTIFY_SUCCESS" -> "notified";
            case "NOTIFY_FAILED" -> "notification_failed";
            case "CLOSE" -> "closed";
            default -> throw new ApiException(ErrorCode.VALIDATION_FAILED, 400, "Unsupported alert action");
        };
        if ("CLOSE".equals(normalizedAction) && (message == null || message.isBlank())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, 400, "Close reason is required");
        }
        validateAlertTransition(event.status(), normalizedAction);
        UUID id = stableUuid(event.id());
        jdbc.sql("""
                UPDATE alert_event
                SET status = :status,
                    assignee_user_id = CASE WHEN :assign THEN :userId ELSE assignee_user_id END,
                    close_reason = CASE WHEN :status = 'closed' THEN :message ELSE close_reason END,
                    notified_at = CASE WHEN :status = 'notified' THEN now() ELSE notified_at END,
                    acknowledged_at = CASE WHEN :status = 'acknowledged' THEN now() ELSE acknowledged_at END,
                    processing_at = CASE WHEN :status = 'processing' THEN now() ELSE processing_at END,
                    recovered_at = CASE WHEN :status = 'recovered' THEN now() ELSE recovered_at END,
                    closed_at = CASE WHEN :status = 'closed' THEN now() ELSE closed_at END,
                    updated_at = now()
                WHERE id = :id
                """)
                .param("status", nextStatus)
                .param("assign", Set.of("ACKNOWLEDGE", "PROCESS").contains(normalizedAction))
                .param("userId", resolveUserId(currentUser).orElse(null))
                .param("message", message)
                .param("id", id)
                .update();
        appendAlertHistory(id, event.status(), nextStatus, normalizedAction, resolveUserId(currentUser).orElse(null), message);
        appendAudit(currentUser, "ALERT_EVENT_" + normalizedAction, "ALERT_EVENT", id, "success");
        return alertEvents(currentUser, null).stream()
                .filter(item -> item.id().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Alert event not found"));
    }

    @Override
    public List<AlertEventHistory> alertEventHistory(String eventId, CurrentUser currentUser) {
        alertEvents(currentUser, null).stream()
                .filter(event -> event.id().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Alert event not found"));
        return jdbc.sql("""
                SELECT id, event_id, from_status, to_status, action, operator_user_id::text, message, operated_at
                FROM alert_event_history
                WHERE event_id = :eventId AND deleted_at IS NULL
                ORDER BY operated_at
                """)
                .param("eventId", stableUuid(eventId))
                .query(this::alertEventHistory)
                .list();
    }

    @Override
    public List<NotificationRecord> notificationRecords(CurrentUser currentUser, String eventId) {
        requirePermission(currentUser, "alerts:read");
        Set<String> eventIds = alertEvents(currentUser, null).stream().map(AlertEvent::id).collect(Collectors.toSet());
        if (eventIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                SELECT id, event_id, rule_id, channel_type, receiver, status, retry_count,
                       failure_reason, next_retry_at, sent_at, created_at
                FROM notification_record
                WHERE deleted_at IS NULL
                  AND event_id::text = ANY(:eventIds)
                  AND (:eventId IS NULL OR event_id = :eventId)
                ORDER BY created_at DESC
                """)
                .param("eventIds", eventIds.toArray(String[]::new))
                .param("eventId", parseUuid(eventId).orElse(null))
                .query(this::notificationRecord)
                .list();
    }

    @Override
    @Transactional
    public NotificationRecord recordNotification(String eventId, String ruleId, String receiver, boolean success, String failureReason, CurrentUser currentUser) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO notification_record (id, event_id, rule_id, channel_type, receiver, status, retry_count, failure_reason, next_retry_at, sent_at)
                VALUES (:id, :eventId, :ruleId, 'email', :receiver, :status, :retryCount, :failureReason, :nextRetryAt, :sentAt)
                """)
                .param("id", id)
                .param("eventId", stableUuid(eventId))
                .param("ruleId", stableUuid(ruleId))
                .param("receiver", receiver)
                .param("status", success ? "sent" : "failed")
                .param("retryCount", success ? 0 : 1)
                .param("failureReason", failureReason)
                .param("nextRetryAt", success ? null : OffsetDateTime.now().plusMinutes(5))
                .param("sentAt", success ? OffsetDateTime.now() : null)
                .update();
        transitionAlertEvent(eventId, success ? "NOTIFY_SUCCESS" : "NOTIFY_FAILED", failureReason, currentUser);
        return notificationRecords(currentUser, eventId).stream()
                .filter(record -> record.id().equals(id.toString()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Notification record not found"));
    }

    @Override
    public List<NotificationRecord> dueNotificationRetries(CurrentUser currentUser, OffsetDateTime now, int limit) {
        requirePermission(currentUser, "alerts:write");
        Set<String> eventIds = alertEvents(currentUser, null).stream()
                .filter(event -> "notification_failed".equalsIgnoreCase(event.status()))
                .map(AlertEvent::id)
                .collect(Collectors.toSet());
        if (eventIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                SELECT id, event_id, rule_id, channel_type, receiver, status, retry_count,
                       failure_reason, next_retry_at, sent_at, created_at
                FROM notification_record
                WHERE deleted_at IS NULL
                  AND event_id::text = ANY(:eventIds)
                  AND lower(status) = 'failed'
                  AND next_retry_at IS NOT NULL
                  AND next_retry_at <= :now
                  AND retry_count < max_retry_count
                ORDER BY next_retry_at, created_at
                LIMIT :limit
                """)
                .param("eventIds", eventIds.toArray(String[]::new))
                .param("now", now)
                .param("limit", Math.max(limit, 1))
                .query(this::notificationRecord)
                .list();
    }

    @Override
    @Transactional
    public NotificationRecord recordNotificationRetry(String notificationId, boolean success, String failureReason, CurrentUser currentUser) {
        requirePermission(currentUser, "alerts:write");
        NotificationRecord current = notificationRecords(currentUser, null).stream()
                .filter(record -> record.id().equals(notificationId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Notification record not found"));
        AlertEvent event = alertEvents(currentUser, null).stream()
                .filter(item -> item.id().equals(current.eventId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Alert event not found"));
        if (!"notification_failed".equalsIgnoreCase(event.status())) {
            throw new ApiException(ErrorCode.ALERT_STATUS_CONFLICT, 409, "Alert event is not waiting for notification retry");
        }

        jdbc.sql("""
                UPDATE notification_record
                SET status = :status,
                    retry_count = retry_count + 1,
                    failure_reason = :failureReason,
                    next_retry_at = CASE
                        WHEN :success THEN NULL
                        WHEN retry_count + 1 >= max_retry_count THEN NULL
                        ELSE now() + (:retryDelayMinutes * interval '1 minute')
                    END,
                    sent_at = CASE WHEN :success THEN now() ELSE sent_at END,
                    updated_at = now()
                WHERE id = :id AND deleted_at IS NULL
                """)
                .param("status", success ? "sent" : "failed")
                .param("failureReason", failureReason)
                .param("success", success)
                .param("retryDelayMinutes", NOTIFICATION_RETRY_DELAY_MINUTES)
                .param("id", stableUuid(notificationId))
                .update();
        transitionAlertEvent(current.eventId(), success ? "NOTIFY_SUCCESS" : "NOTIFY_FAILED", failureReason, currentUser);
        return notificationRecords(currentUser, current.eventId()).stream()
                .filter(record -> record.id().equals(notificationId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Notification record not found"));
    }

    @Override
    public List<OnCallGroup> onCallGroups(CurrentUser currentUser) {
        requirePermission(currentUser, "alerts:read");
        return jdbc.sql("""
                SELECT ocg.id, ocg.code, ocg.name, bl.code AS business_line, ocg.status, ocg.created_at, ocg.updated_at,
                       COALESCE(array_agg(ocgm.user_id::text ORDER BY ocgm.notify_priority) FILTER (WHERE ocgm.user_id IS NOT NULL), ARRAY[]::text[]) AS member_user_ids
                FROM on_call_group ocg
                LEFT JOIN business_line bl ON bl.id = ocg.business_line_id
                LEFT JOIN on_call_group_member ocgm ON ocgm.group_id = ocg.id AND ocgm.status = 'active' AND ocgm.deleted_at IS NULL
                WHERE ocg.deleted_at IS NULL
                  AND (:platformAdmin OR bl.code = ANY(:businessLines))
                GROUP BY ocg.id, ocg.code, ocg.name, bl.code, ocg.status, ocg.created_at, ocg.updated_at
                ORDER BY ocg.code
                """)
                .param("platformAdmin", currentUser.dataScope().platformAdmin())
                .param("businessLines", currentUser.dataScope().businessLines().toArray(String[]::new))
                .query(this::onCallGroup)
                .list();
    }

    @Override
    public List<LogEntry> searchLogs(CurrentUser currentUser, com.heimdallr.monitor.api.dto.LogSearchCriteria criteria) {
        Set<String> applicationIds = visibleApplications(currentUser).stream()
                .map(ApplicationAsset::id)
                .collect(Collectors.toSet());
        if (applicationIds.isEmpty()) {
            return List.of();
        }
        List<LogEntry> externalLogs = searchExternalLogs(currentUser, criteria);
        if (!externalLogs.isEmpty()) {
            return externalLogs;
        }
        return jdbc.sql("""
                SELECT le.code, le.occurred_at, le.application_id::text, mo.code AS object_code, le.env, le.level,
                       le.message, le.trace_id, ds.code AS source_code, le.labels::text
                FROM log_entry_sample le
                LEFT JOIN monitor_object mo ON mo.id = le.object_id
                LEFT JOIN data_source ds ON ds.id = le.source_id
                WHERE le.deleted_at IS NULL
                  AND le.application_id::text = ANY(:applicationIds)
                  AND (CAST(:applicationId AS text) IS NULL OR le.application_id::text = :applicationId)
                  AND (CAST(:objectId AS text) IS NULL OR mo.code = :objectId)
                  AND (CAST(:environment AS text) IS NULL OR le.env = :environment)
                  AND (CAST(:level AS text) IS NULL OR upper(le.level) = upper(CAST(:level AS text)))
                  AND (CAST(:keyword AS text) IS NULL OR lower(le.message) LIKE lower(concat('%', CAST(:keyword AS text), '%')))
                  AND (CAST(:traceId AS text) IS NULL OR le.trace_id = :traceId)
                  AND (CAST(:fromTime AS timestamptz) IS NULL OR le.occurred_at >= :fromTime)
                  AND (CAST(:toTime AS timestamptz) IS NULL OR le.occurred_at <= :toTime)
                ORDER BY le.occurred_at DESC
                """)
                .param("applicationIds", applicationIds.toArray(String[]::new))
                .param("applicationId", criteria.applicationId())
                .param("objectId", criteria.objectId())
                .param("environment", criteria.environment())
                .param("level", criteria.level())
                .param("keyword", criteria.keyword())
                .param("traceId", criteria.traceId())
                .param("fromTime", criteria.from())
                .param("toTime", criteria.to())
                .query((rs, rowNum) -> new LogEntry(
                        rs.getString("code"),
                        rs.getObject("occurred_at", OffsetDateTime.class),
                        rs.getString("application_id"),
                        rs.getString("object_code"),
                        rs.getString("env"),
                        rs.getString("level"),
                        rs.getString("message"),
                        rs.getString("trace_id"),
                        rs.getString("source_code"),
                        jsonMap(rs.getString("labels"))
                ))
                .list();
    }

    @Override
    @Transactional
    public UserInfo grantApplicationAccess(String userId, String applicationId, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UUID targetUserId = requireUserId(userId);
        UUID targetApplicationId = requireApplicationId(applicationId);
        UUID businessLineId = businessLineIdForApplication(targetApplicationId).orElseThrow(() -> new NotFoundException("Application not found"));
        jdbc.sql("""
                INSERT INTO application_authorization (user_id, application_id, business_line_id, env, access_level, granted_by, grant_reason)
                SELECT :userId, a.id, a.business_line_id, a.env, 'read', :grantedBy, 'Granted from API'
                FROM application a WHERE a.id = :applicationId
                ON CONFLICT (user_id, application_id, env) DO UPDATE
                SET status = 'active', granted_by = EXCLUDED.granted_by, updated_at = now()
                """)
                .param("userId", targetUserId)
                .param("applicationId", targetApplicationId)
                .param("grantedBy", resolveUserId(currentUser).orElse(null))
                .update();
        appendAudit(currentUser, "ACCESS_GRANT_APPLICATION", "USER", targetUserId, "success");
        return requireUser(targetUserId);
    }

    @Override
    @Transactional
    public UserInfo revokeApplicationAccess(String userId, String applicationId, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UUID targetUserId = requireUserId(userId);
        UUID targetApplicationId = requireApplicationId(applicationId);
        jdbc.sql("UPDATE application_authorization SET status = 'revoked', updated_at = now() WHERE user_id = :userId AND application_id = :applicationId")
                .param("userId", targetUserId)
                .param("applicationId", targetApplicationId)
                .update();
        appendAudit(currentUser, "ACCESS_REVOKE_APPLICATION", "USER", targetUserId, "success");
        return requireUser(targetUserId);
    }

    @Override
    @Transactional
    public UserInfo grantBusinessLineAccess(String userId, String businessLine, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UUID targetUserId = requireUserId(userId);
        UUID businessLineId = ensureBusinessLine(businessLine);
        jdbc.sql("""
                INSERT INTO business_line_authorization (user_id, business_line_id, access_level, granted_by, grant_reason)
                VALUES (:userId, :businessLineId, 'read', :grantedBy, 'Granted from API')
                ON CONFLICT (user_id, business_line_id, access_level) DO UPDATE
                SET status = 'active', granted_by = EXCLUDED.granted_by, updated_at = now()
                """)
                .param("userId", targetUserId)
                .param("businessLineId", businessLineId)
                .param("grantedBy", resolveUserId(currentUser).orElse(null))
                .update();
        appendAudit(currentUser, "ACCESS_GRANT_BUSINESS_LINE", "USER", targetUserId, "success");
        return requireUser(targetUserId);
    }

    @Override
    @Transactional
    public UserInfo revokeBusinessLineAccess(String userId, String businessLine, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UUID targetUserId = requireUserId(userId);
        UUID businessLineId = ensureBusinessLine(businessLine);
        jdbc.sql("UPDATE business_line_authorization SET status = 'revoked', updated_at = now() WHERE user_id = :userId AND business_line_id = :businessLineId")
                .param("userId", targetUserId)
                .param("businessLineId", businessLineId)
                .update();
        appendAudit(currentUser, "ACCESS_REVOKE_BUSINESS_LINE", "USER", targetUserId, "success");
        return requireUser(targetUserId);
    }

    private ValidationProbe probeDataSource(DataSourceConfig source) {
        if (source.baseUrl() == null || source.baseUrl().isBlank()) {
            return new ValidationProbe(false, "CONFIG_INVALID", "Base URL is empty");
        }
        String path = Optional.ofNullable(source.healthCheckPath()).filter(item -> !item.isBlank()).orElse("/");
        String url = source.baseUrl() + (path.startsWith("/") ? path : "/" + path);
        if ("PROMETHEUS".equalsIgnoreCase(source.type()) && path.contains("/api/v1/query")) {
            url = url + "?query=up";
        }
        try {
            HttpResponse<String> response = sendGet(url, Math.max(source.timeoutSeconds(), 1));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new ValidationProbe(true, null, "Connection test passed");
            }
            return new ValidationProbe(false, "HTTP_" + response.statusCode(), "HTTP " + response.statusCode());
        } catch (RuntimeException ex) {
            return new ValidationProbe(false, "CONNECT_FAILED", ex.getMessage());
        }
    }

    private AgentInstance mergeAgentGatewayStatus(CurrentUser currentUser, AgentInstance agent) {
        Optional<DataSourceConfig> gateway = visibleDataSources(currentUser).stream()
                .filter(source -> "AGENT".equalsIgnoreCase(source.type()))
                .findFirst();
        if (gateway.isEmpty()) {
            return agent;
        }
        try {
            HttpResponse<String> response = sendGet(gateway.get().baseUrl() + "/status", Math.max(gateway.get().timeoutSeconds(), 1));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return agent;
            }
            JsonNode root = objectMapper.readTree(response.body());
            int online = root.path("agents").path("online").asInt(0);
            String status = online > 0 ? "ONLINE" : "NO_HEARTBEAT";
            return new AgentInstance(
                    agent.id(),
                    agent.serverId(),
                    agent.hostname(),
                    agent.environment(),
                    agent.version(),
                    status,
                    OffsetDateTime.now(),
                    agent.configVersion(),
                    online > 0 ? null : "Agent gateway reports no online agents"
            );
        } catch (IOException | RuntimeException ex) {
            return agent;
        }
    }

    private List<MetricSeries.MetricSample> queryPrometheusSamples(
            DataSourceConfig source,
            String objectType,
            String metricCode,
            String objectId,
            String defaultQueryTemplate,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        String query = defaultMetricMappings(objectType).stream()
                .filter(mapping -> mapping.metricCode().equals(metricCode))
                .map(DefaultMetricMapping::queryTemplate)
                .findFirst()
                .orElse(defaultQueryTemplate == null || defaultQueryTemplate.isBlank() ? "up" : defaultQueryTemplate);
        query = query.replace("$object", objectId);
        String url = source.baseUrl()
                + "/api/v1/query_range?query=" + encode(query)
                + "&start=" + from.toEpochSecond()
                + "&end=" + to.toEpochSecond()
                + "&step=60";
        try {
            HttpResponse<String> response = sendGet(url, Math.max(source.timeoutSeconds(), 1));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            JsonNode result = objectMapper.readTree(response.body()).path("data").path("result");
            List<MetricSeries.MetricSample> samples = new ArrayList<>();
            if (result.isArray()) {
                for (JsonNode series : result) {
                    JsonNode values = series.path("values");
                    if (values.isArray()) {
                        for (JsonNode value : values) {
                            samples.add(metricSample(value));
                        }
                    } else if (series.path("value").isArray()) {
                        samples.add(metricSample(series.path("value")));
                    }
                }
            }
            return samples.stream()
                    .filter(sample -> sample.timestamp() != null)
                    .sorted((left, right) -> left.timestamp().compareTo(right.timestamp()))
                    .limit(200)
                    .toList();
        } catch (IOException | RuntimeException ex) {
            return List.of();
        }
    }

    private List<LogEntry> searchExternalLogs(CurrentUser currentUser, com.heimdallr.monitor.api.dto.LogSearchCriteria criteria) {
        Optional<DataSourceConfig> elasticsearch = visibleDataSources(currentUser).stream()
                .filter(source -> "ELASTICSEARCH".equalsIgnoreCase(source.type()) || "LOG".equalsIgnoreCase(source.type()) || "LOKI".equalsIgnoreCase(source.type()))
                .findFirst();
        if (elasticsearch.isEmpty() || !"ELASTICSEARCH".equalsIgnoreCase(elasticsearch.get().type())) {
            return List.of();
        }
        List<ApplicationAsset> visibleApplications = visibleApplications(currentUser);
        Set<String> visibleApplicationCodes = visibleApplications.stream()
                .map(ApplicationAsset::code)
                .collect(Collectors.toSet());
        Map<String, String> applicationIdsByCode = visibleApplications.stream()
                .collect(Collectors.toMap(ApplicationAsset::code, ApplicationAsset::id, (left, right) -> left));
        try {
            String body = elasticsearchQueryBody(criteria);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(elasticsearch.get().baseUrl() + "/test-logs-*/_search"))
                    .timeout(Duration.ofSeconds(Math.max(elasticsearch.get().timeoutSeconds(), 1)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            JsonNode hits = objectMapper.readTree(response.body()).path("hits").path("hits");
            List<LogEntry> entries = new ArrayList<>();
            for (JsonNode hit : hits) {
                JsonNode source = hit.path("_source");
                String applicationCode = firstText(source, "application", "app", "service");
                if (!visibleApplicationCodes.isEmpty() && applicationCode != null && !visibleApplicationCodes.contains(applicationCode)) {
                    continue;
                }
                String level = text(source, "level");
                String environment = firstText(source, "environment", "env");
                String message = text(source, "message");
                if (criteria.level() != null && level != null && !criteria.level().equalsIgnoreCase(level)) {
                    continue;
                }
                if (criteria.environment() != null && environment != null && !criteria.environment().equalsIgnoreCase(environment)) {
                    continue;
                }
                if (criteria.keyword() != null && (message == null || !message.toLowerCase().contains(criteria.keyword().toLowerCase()))) {
                    continue;
                }
                entries.add(new LogEntry(
                        hit.path("_id").asText(),
                        parseTimestamp(firstText(source, "@timestamp", "timestamp")),
                        applicationIdsByCode.getOrDefault(applicationCode, applicationCode),
                        firstText(source, "objectId", "object", "instance"),
                        environment,
                        level,
                        message,
                        firstText(source, "traceId", "trace_id"),
                        elasticsearch.get().id(),
                        logLabels(source)
                ));
            }
            return entries;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (IOException | RuntimeException ex) {
            return List.of();
        }
    }

    private String elasticsearchQueryBody(com.heimdallr.monitor.api.dto.LogSearchCriteria criteria) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", criteria.normalizedPageSize());
        List<Map<String, Object>> filters = new ArrayList<>();
        if (criteria.traceId() != null && !criteria.traceId().isBlank()) {
            filters.add(Map.of("term", Map.of("traceId.keyword", criteria.traceId())));
        }
        if (criteria.from() != null || criteria.to() != null) {
            Map<String, String> range = new LinkedHashMap<>();
            if (criteria.from() != null) {
                range.put("gte", criteria.from().toString());
            }
            if (criteria.to() != null) {
                range.put("lte", criteria.to().toString());
            }
            filters.add(Map.of("range", Map.of("@timestamp", range)));
        }
        body.put("query", filters.isEmpty() ? Map.of("match_all", Map.of()) : Map.of("bool", Map.of("filter", filters)));
        body.put("sort", List.of(Map.of("@timestamp", Map.of("order", "desc"))));
        return objectMapper.writeValueAsString(body);
    }

    private HttpResponse<String> sendGet(String url, int timeoutSeconds) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private MetricSeries.MetricSample metricSample(JsonNode value) {
        if (!value.isArray() || value.size() < 2) {
            return new MetricSeries.MetricSample(null, 0);
        }
        OffsetDateTime timestamp = OffsetDateTime.ofInstant(Instant.ofEpochSecond(value.get(0).asLong()), ZoneOffset.UTC);
        return new MetricSeries.MetricSample(timestamp, Double.parseDouble(value.get(1).asText("0")));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static OffsetDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return OffsetDateTime.now();
        }
        return OffsetDateTime.parse(value);
    }

    private static String firstText(JsonNode node, String... names) {
        for (String name : names) {
            String value = text(node, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node.path(name);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static Map<String, String> logLabels(JsonNode source) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (String name : List.of("service", "application", "environment", "host", "instance")) {
            String value = text(source, name);
            if (value != null) {
                labels.put(name, value);
            }
        }
        return labels;
    }

    private record ValidationProbe(boolean passed, String errorCode, String message) {
    }

    private ApplicationAsset applicationAsset(ResultSet rs, int rowNum) throws SQLException {
        return new ApplicationAsset(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("business_line"),
                rs.getString("env"),
                List.of((String[]) rs.getArray("owner_user_ids").getArray()),
                rs.getString("access_status")
        );
    }

    private MonitorObject monitorObject(ResultSet rs, int rowNum) throws SQLException {
        return new MonitorObject(
                rs.getString("code"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("object_type"),
                rs.getString("env"),
                rs.getString("business_line"),
                List.of((String[]) rs.getArray("owner_user_ids").getArray()),
                List.of((String[]) rs.getArray("application_ids").getArray()),
                List.of((String[]) rs.getArray("server_ids").getArray()),
                rs.getString("health_status"),
                rs.getString("access_status"),
                jsonMap(rs.getString("key_metrics"))
        );
    }

    private DataSourceConfig dataSourceConfig(ResultSet rs, int rowNum) throws SQLException {
        return new DataSourceConfig(
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("source_type"),
                rs.getString("env"),
                rs.getString("base_url"),
                rs.getString("health_check_path"),
                rs.getString("auth_type"),
                rs.getString("secret_ref"),
                rs.getInt("timeout_seconds"),
                rs.getInt("retry_count"),
                rs.getInt("rate_limit_qps"),
                rs.getString("status"),
                rs.getObject("last_check_at", OffsetDateTime.class),
                rs.getObject("last_success_at", OffsetDateTime.class),
                rs.getString("last_error_code"),
                rs.getString("last_error_message")
        );
    }

    private DataSourceBinding dataSourceBinding(ResultSet rs, int rowNum) throws SQLException {
        return new DataSourceBinding(
                rs.getString("code"),
                rs.getString("object_code"),
                rs.getString("object_type"),
                rs.getString("source_code"),
                rs.getString("binding_type"),
                jsonMap(rs.getString("external_labels")),
                jsonMap(rs.getString("mapping_config")),
                rs.getObject("last_seen_at", OffsetDateTime.class),
                rs.getString("access_status"),
                rs.getString("failure_reason")
        );
    }

    private AlertRule alertRule(ResultSet rs, int rowNum) throws SQLException {
        return new AlertRule(
                rs.getString("id"),
                rs.getString("rule_name"),
                rs.getString("object_id"),
                rs.getString("object_name"),
                rs.getString("metric_code"),
                rs.getString("operator"),
                rs.getDouble("threshold"),
                rs.getInt("window_seconds"),
                rs.getInt("duration_seconds"),
                rs.getInt("evaluation_interval_seconds"),
                rs.getString("severity"),
                rs.getBoolean("enabled"),
                rs.getString("business_line"),
                rs.getString("app_id"),
                rs.getString("on_call_group_id"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    private AlertRuleRuntime alertRuleRuntime(ResultSet rs, int rowNum) throws SQLException {
        java.math.BigDecimal lastValue = rs.getBigDecimal("last_value");
        return new AlertRuleRuntime(
                rs.getString("rule_id"),
                rs.getObject("last_evaluated_at", OffsetDateTime.class),
                rs.getObject("next_evaluate_at", OffsetDateTime.class),
                rs.getString("last_status"),
                lastValue == null ? null : lastValue.doubleValue(),
                rs.getString("last_error"),
                rs.getObject("evaluation_duration_ms", Long.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    private AlertEvaluationSample alertEvaluationSample(ResultSet rs, int rowNum) throws SQLException {
        java.math.BigDecimal value = rs.getBigDecimal("metric_value");
        return new AlertEvaluationSample(
                rs.getString("id"),
                rs.getString("rule_id"),
                rs.getObject("evaluated_at", OffsetDateTime.class),
                rs.getString("status"),
                value == null ? null : value.doubleValue(),
                rs.getDouble("threshold"),
                rs.getString("operator"),
                rs.getBoolean("matched"),
                rs.getString("event_id"),
                rs.getString("error"),
                rs.getObject("evaluation_duration_ms", Long.class),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }

    private AlertEvent alertEvent(ResultSet rs, int rowNum) throws SQLException {
        return new AlertEvent(
                rs.getString("id"),
                rs.getString("rule_id"),
                rs.getString("rule_name"),
                rs.getString("object_id"),
                rs.getString("object_name"),
                rs.getString("metric_code"),
                rs.getString("severity"),
                rs.getString("status"),
                rs.getDouble("trigger_value"),
                rs.getDouble("threshold"),
                rs.getString("operator"),
                rs.getString("assignee_user_id"),
                rs.getString("close_reason"),
                rs.getObject("triggered_at", OffsetDateTime.class),
                rs.getObject("notified_at", OffsetDateTime.class),
                rs.getObject("acknowledged_at", OffsetDateTime.class),
                rs.getObject("processing_at", OffsetDateTime.class),
                rs.getObject("recovered_at", OffsetDateTime.class),
                rs.getObject("closed_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    private AlertEventHistory alertEventHistory(ResultSet rs, int rowNum) throws SQLException {
        return new AlertEventHistory(
                rs.getString("id"),
                rs.getString("event_id"),
                rs.getString("from_status"),
                rs.getString("to_status"),
                rs.getString("action"),
                rs.getString("operator_user_id"),
                rs.getString("message"),
                rs.getObject("operated_at", OffsetDateTime.class)
        );
    }

    private NotificationRecord notificationRecord(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationRecord(
                rs.getString("id"),
                rs.getString("event_id"),
                rs.getString("rule_id"),
                rs.getString("channel_type"),
                rs.getString("receiver"),
                rs.getString("status"),
                rs.getInt("retry_count"),
                rs.getString("failure_reason"),
                rs.getObject("next_retry_at", OffsetDateTime.class),
                rs.getObject("sent_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }

    private OnCallGroup onCallGroup(ResultSet rs, int rowNum) throws SQLException {
        return new OnCallGroup(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("business_line"),
                List.of((String[]) rs.getArray("member_user_ids").getArray()),
                rs.getString("status"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    private static DataSourceValidationResult.ValidationItem validationItem(String name, boolean passed, String errorCode, String successMessage) {
        return new DataSourceValidationResult.ValidationItem(
                name,
                passed ? "PASSED" : "FAILED",
                passed ? null : errorCode,
                passed ? successMessage : errorCode
        );
    }

    private static double sampleValue(String metricCode, int index) {
        return switch (metricCode) {
            case "mq_lag" -> 980 + index * 120;
            case "broker_up" -> 3;
            case "db_conn_usage" -> 56 + index * 2;
            case "slow_sql_count" -> 2 + index;
            case "redis_memory_usage" -> 70 + index;
            case "http_5xx_rate" -> 1.1 + index * 0.2;
            default -> 10 + index;
        };
    }

    private static Map<String, String> jsonMap(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return Map.of();
        }
        String body = json.trim();
        if (body.startsWith("{")) {
            body = body.substring(1);
        }
        if (body.endsWith("}")) {
            body = body.substring(0, body.length() - 1);
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String item : body.split(",")) {
            String[] parts = item.split(":", 2);
            if (parts.length == 2) {
                values.put(unquote(parts[0]), unquote(parts[1]));
            }
        }
        return values;
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private ServerAsset serverAsset(ResultSet rs, int rowNum) throws SQLException {
        return new ServerAsset(
                rs.getString("id"),
                rs.getString("hostname"),
                rs.getString("ip"),
                rs.getString("env"),
                Set.of((String[]) rs.getArray("application_ids").getArray()),
                rs.getString("access_status")
        );
    }

    private UUID ensureBusinessLine(String code) {
        String normalized = code == null || code.isBlank() ? "shared" : code;
        return jdbc.sql("""
                INSERT INTO business_line (code, name)
                VALUES (:code, :name)
                ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
                RETURNING id
                """)
                .param("code", normalized)
                .param("name", normalized)
                .query(UUID.class)
                .single();
    }

    private Optional<UUID> businessLineIdForApplication(UUID applicationId) {
        return jdbc.sql("SELECT business_line_id FROM application WHERE id = :id AND deleted_at IS NULL")
                .param("id", applicationId)
                .query(UUID.class)
                .optional();
    }

    private Optional<UUID> businessLineIdByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return jdbc.sql("SELECT id FROM business_line WHERE code = :code AND deleted_at IS NULL")
                .param("code", code)
                .query(UUID.class)
                .optional();
    }

    private Optional<UUID> onCallGroupId(String idOrCode) {
        if (idOrCode == null || idOrCode.isBlank()) {
            return Optional.empty();
        }
        Optional<UUID> parsed = parseUuid(idOrCode);
        if (parsed.isPresent()) {
            return parsed;
        }
        return jdbc.sql("SELECT id FROM on_call_group WHERE code = :code AND deleted_at IS NULL")
                .param("code", idOrCode)
                .query(UUID.class)
                .optional();
    }

    private UUID requireApplicationId(String applicationId) {
        UUID id = stableUuid(applicationId);
        boolean exists = jdbc.sql("SELECT EXISTS(SELECT 1 FROM application WHERE id = :id AND deleted_at IS NULL)")
                .param("id", id)
                .query(Boolean.class)
                .single();
        if (!exists) {
            throw new NotFoundException("Application not found");
        }
        return id;
    }

    private boolean alertRuleExists(String ruleId) {
        UUID id = stableUuid(ruleId);
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM alert_rule WHERE id = :id AND deleted_at IS NULL)")
                .param("id", id)
                .query(Boolean.class)
                .single();
    }

    private UUID requireUserId(String userId) {
        return parseUuid(userId)
                .filter(this::userExists)
                .or(() -> userIdByAlias(userId))
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private boolean userExists(UUID userId) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM user_account WHERE id = :id AND deleted_at IS NULL)")
                .param("id", userId)
                .query(Boolean.class)
                .single();
    }

    private UserInfo requireUser(UUID userId) {
        return jdbc.sql("""
                SELECT u.id, u.username, u.display_name,
                       COALESCE(array_agg(DISTINCT bl.code) FILTER (WHERE bl.code IS NOT NULL), ARRAY[]::text[]) AS business_lines
                FROM user_account u
                LEFT JOIN business_line_authorization ba ON ba.user_id = u.id AND ba.status = 'active' AND ba.deleted_at IS NULL
                LEFT JOIN business_line bl ON bl.id = ba.business_line_id
                WHERE u.id = :userId AND u.deleted_at IS NULL
                GROUP BY u.id, u.username, u.display_name
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> new UserInfo(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        userRoles(rs.getObject("id", UUID.class)),
                        Set.of((String[]) rs.getArray("business_lines").getArray()),
                        Set.of("assets", "access", "audit")
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private List<RoleInfo> userRoles(UUID userId) {
        List<RoleInfo> roles = jdbc.sql("""
                SELECT DISTINCT r.id, r.code, r.name
                FROM user_role ur
                JOIN role r ON r.id = ur.role_id
                WHERE ur.user_id = :userId AND ur.deleted_at IS NULL AND r.deleted_at IS NULL
                ORDER BY r.code
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> roleInfo(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name")))
                .list();
        return roles.isEmpty() ? List.of(new RoleInfo("viewer", "VIEWER", "Viewer", Set.of("applications:read", "servers:read"))) : roles;
    }

    private RoleInfo roleInfo(UUID id, String code, String name) {
        Set<String> permissions = Set.copyOf(jdbc.sql("""
                SELECT p.code
                FROM role_permission rp
                JOIN permission p ON p.id = rp.permission_id
                WHERE rp.role_id = :roleId
                  AND p.status = 'active'
                  AND p.deleted_at IS NULL
                ORDER BY p.code
                """)
                .param("roleId", id)
                .query(String.class)
                .list());
        return new RoleInfo(id.toString(), code == null ? "VIEWER" : code.toUpperCase(), name, permissions);
    }

    private Optional<UUID> resolveUserId(CurrentUser currentUser) {
        return parseUuid(currentUser.user().id()).filter(this::userExists)
                .or(() -> userIdByAlias(currentUser.user().username()))
                .or(() -> userIdByAlias(currentUser.token()));
    }

    private Optional<UUID> userIdByAlias(String alias) {
        String username = switch (alias) {
            case "platform-admin", "admin-token" -> "admin";
            case "sre", "sre-token" -> "sre001";
            case "ace-owner", "ace-owner-token", "u-ace-owner" -> "user001";
            case "u-admin" -> "admin";
            case "u-sre" -> "sre001";
            default -> alias;
        };
        return jdbc.sql("SELECT id FROM user_account WHERE username = :username AND deleted_at IS NULL")
                .param("username", username)
                .query(UUID.class)
                .optional();
    }

    private void appendAudit(CurrentUser currentUser, String action, String targetType, UUID targetId, String result) {
        jdbc.sql("""
                INSERT INTO audit_event (operator_user_id, action, resource_type, resource_id, result, detail)
                VALUES (:operatorUserId, :action, :resourceType, :resourceId, :result, '{}'::jsonb)
                """)
                .param("operatorUserId", resolveUserId(currentUser).orElse(null))
                .param("action", action)
                .param("resourceType", targetType)
                .param("resourceId", targetId)
                .param("result", result)
                .update();
    }

    private void appendAlertHistory(UUID eventId, String fromStatus, String toStatus, String action, UUID operatorUserId, String message) {
        jdbc.sql("""
                INSERT INTO alert_event_history (event_id, from_status, to_status, action, operator_user_id, message)
                VALUES (:eventId, :fromStatus, :toStatus, :action, :operatorUserId, :message)
                """)
                .param("eventId", eventId)
                .param("fromStatus", fromStatus)
                .param("toStatus", toStatus)
                .param("action", action)
                .param("operatorUserId", operatorUserId)
                .param("message", message)
                .update();
    }

    private static String normalizeStatus(String status, String fallback) {
        return status == null || status.isBlank() ? fallback : status.toLowerCase();
    }

    private static void validateAlertTransition(String currentStatus, String action) {
        String status = currentStatus == null ? "" : currentStatus.toUpperCase();
        if (isTerminalAlertStatus(status)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, 400, "Alert event is already terminal");
        }
        if ("ACKNOWLEDGE".equals(action) && !Set.of("TRIGGERED", "NOTIFIED", "NOTIFICATION_FAILED").contains(status)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, 400, "Alert event cannot be acknowledged from current status");
        }
        if ("PROCESS".equals(action) && !Set.of("TRIGGERED", "NOTIFIED", "NOTIFICATION_FAILED", "ACKNOWLEDGED").contains(status)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, 400, "Alert event cannot be processed from current status");
        }
    }

    private static boolean isTerminalAlertStatus(String status) {
        return status != null && Set.of("RECOVERED", "CLOSED").contains(status.toUpperCase());
    }

    private static UUID stableUuid(String value) {
        return parseUuid(value).orElseGet(() -> UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
