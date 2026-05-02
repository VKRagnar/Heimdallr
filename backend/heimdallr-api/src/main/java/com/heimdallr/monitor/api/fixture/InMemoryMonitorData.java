package com.heimdallr.monitor.api.fixture;

import com.heimdallr.monitor.api.repository.MonitorData;
import com.heimdallr.monitor.common.domain.api.ErrorCode;
import com.heimdallr.monitor.common.domain.exception.ApiException;
import com.heimdallr.monitor.common.domain.exception.ForbiddenException;
import com.heimdallr.monitor.common.domain.exception.NotFoundException;
import com.heimdallr.monitor.common.domain.model.ApplicationAsset;
import com.heimdallr.monitor.common.domain.model.ApplicationInstance;
import com.heimdallr.monitor.common.domain.model.AuditEvent;
import com.heimdallr.monitor.common.domain.model.AgentInstance;
import com.heimdallr.monitor.common.domain.model.DataSourceBinding;
import com.heimdallr.monitor.common.domain.model.DataSourceConfig;
import com.heimdallr.monitor.common.domain.model.DataSourceValidationResult;
import com.heimdallr.monitor.common.domain.model.DefaultMetricMapping;
import com.heimdallr.monitor.common.domain.model.LogEntry;
import com.heimdallr.monitor.common.domain.model.MetricDefinition;
import com.heimdallr.monitor.common.domain.model.MetricSeries;
import com.heimdallr.monitor.common.domain.model.MonitorObject;
import com.heimdallr.monitor.common.domain.model.RoleInfo;
import com.heimdallr.monitor.common.domain.model.ServerAsset;
import com.heimdallr.monitor.common.domain.model.UserInfo;
import com.heimdallr.monitor.common.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!db")
public class InMemoryMonitorData implements MonitorData {
    private final List<ApplicationAsset> applications = new CopyOnWriteArrayList<>(List.of(
            new ApplicationAsset("app-ace", "ACE", "ACE Trading", "trade", "prod", List.of("u-ace-owner"), "CONNECTED"),
            new ApplicationAsset("app-ipro", "IPRO", "iPro Portal", "trade", "staging", List.of("u-sre"), "DEGRADED"),
            new ApplicationAsset("app-cms", "CMS", "Content Management", "content", "prod", List.of("u-cms-owner"), "CONNECTED"),
            new ApplicationAsset("app-grafana", "GRAFANA", "Grafana", "core-platform", "prod", List.of("u-sre"), "CONNECTED")
    ));

    private final List<ApplicationInstance> instances = List.of(
            new ApplicationInstance("inst-ace-1", "app-ace", "srv-ace-1", "ace-api-01", "prod", "ONLINE"),
            new ApplicationInstance("inst-ace-2", "app-ace", "srv-ace-2", "ace-api-02", "prod", "ONLINE"),
            new ApplicationInstance("inst-ipro-1", "app-ipro", "srv-ipro-1", "ipro-web-01", "staging", "WARN"),
            new ApplicationInstance("inst-cms-1", "app-cms", "srv-cms-1", "cms-api-01", "prod", "ONLINE"),
            new ApplicationInstance("inst-grafana-1", "app-grafana", "srv-grafana-1", "grafana-01", "prod", "ONLINE")
    );

    private final List<ServerAsset> servers = new CopyOnWriteArrayList<>(List.of(
            new ServerAsset("srv-ace-1", "ace-api-01", "10.10.1.11", "prod", Set.of("app-ace"), "CONNECTED"),
            new ServerAsset("srv-ace-2", "ace-api-02", "10.10.1.12", "prod", Set.of("app-ace"), "CONNECTED"),
            new ServerAsset("srv-ipro-1", "ipro-web-01", "10.10.2.21", "staging", Set.of("app-ipro"), "DEGRADED"),
            new ServerAsset("srv-cms-1", "cms-api-01", "10.10.3.31", "prod", Set.of("app-cms"), "CONNECTED"),
            new ServerAsset("srv-grafana-1", "grafana-01", "10.10.9.41", "prod", Set.of("app-grafana"), "CONNECTED")
    ));

    private final List<DataSourceConfig> dataSources = new CopyOnWriteArrayList<>(List.of(
            new DataSourceConfig("ds-prom-prod", "prometheus-prod", "PROMETHEUS", "prod", "https://prometheus.example.com", "/api/v1/query", "token", "secret/prometheus-prod-token", 5, 2, 20, "ENABLED", OffsetDateTime.now().minusMinutes(5), OffsetDateTime.now().minusMinutes(5), null, null),
            new DataSourceConfig("ds-grafana-prod", "grafana-prod", "GRAFANA", "prod", "https://grafana.example.com", "/api/health", "token", "secret/grafana-prod-token", 5, 1, 20, "ENABLED", OffsetDateTime.now().minusMinutes(5), OffsetDateTime.now().minusMinutes(5), null, null),
            new DataSourceConfig("ds-loki-prod", "loki-prod", "LOKI", "prod", "https://loki.example.com", "/loki/api/v1/query_range", "basic", "secret/loki-prod-basic", 8, 1, 10, "ENABLED", OffsetDateTime.now().minusMinutes(6), OffsetDateTime.now().minusMinutes(6), null, null),
            new DataSourceConfig("ds-agent-prod", "agent-gateway-prod", "AGENT", "prod", "https://agent-gateway.example.com", "/health", "token", "secret/agent-prod-token", 3, 1, 50, "ENABLED", OffsetDateTime.now().minusMinutes(2), OffsetDateTime.now().minusMinutes(2), null, null),
            new DataSourceConfig("ds-kafka-prod", "kafka-exporter-prod", "KAFKA", "prod", "https://kafka-exporter.example.com", "/metrics", "token", "secret/kafka-prod-token", 5, 2, 20, "ENABLED", OffsetDateTime.now().minusMinutes(5), OffsetDateTime.now().minusMinutes(5), null, null),
            new DataSourceConfig("ds-postgresql-prod", "postgres-exporter-prod", "POSTGRESQL", "prod", "https://postgres-exporter.example.com", "/metrics", "token", "secret/postgresql-prod-token", 5, 2, 20, "ENABLED", OffsetDateTime.now().minusMinutes(5), OffsetDateTime.now().minusMinutes(5), null, null),
            new DataSourceConfig("ds-prom-staging", "prometheus-staging", "PROMETHEUS", "staging", "https://prometheus-staging.example.com", "/api/v1/query", "token", "secret/prometheus-staging-token", 5, 2, 15, "UNHEALTHY", OffsetDateTime.now().minusMinutes(4), OffsetDateTime.now().minusHours(3), "CONNECT_TIMEOUT", "Connection timed out")
    ));

    private final List<MonitorObject> monitorObjects = List.of(
            new MonitorObject("obj-kafka-orders", "KAFKA_ORDERS", "orders-kafka", "KAFKA", "prod", "trade", List.of("u-sre"), List.of("app-ace", "app-ipro"), List.of("srv-ace-1"), "WARN", "CONNECTED", Map.of("mq_lag", "1280", "broker_up", "3/3")),
            new MonitorObject("obj-pg-ace", "PG_ACE", "ace-postgresql", "POSTGRESQL", "prod", "trade", List.of("u-ace-owner", "u-sre"), List.of("app-ace"), List.of("srv-ace-2"), "HEALTHY", "CONNECTED", Map.of("db_conn_usage", "61%", "slow_sql_count", "3")),
            new MonitorObject("obj-redis-cms", "REDIS_CMS", "cms-redis", "REDIS", "prod", "content", List.of("u-cms-owner"), List.of("app-cms"), List.of("srv-cms-1"), "HEALTHY", "PARTIAL_CONNECTED", Map.of("redis_memory_usage", "72%", "cache_hit_rate", "96.8%")),
            new MonitorObject("obj-ipro-api", "IPRO_API", "ipro-api", "APPLICATION", "staging", "trade", List.of("u-sre"), List.of("app-ipro"), List.of("srv-ipro-1"), "WARN", "SOURCE_UNAVAILABLE", Map.of("http_5xx_rate", "1.8%", "p95_latency", "860ms"))
    );

    private final List<DataSourceBinding> dataSourceBindings = List.of(
            new DataSourceBinding("bind-kafka-metrics", "obj-kafka-orders", "KAFKA", "ds-prom-prod", "METRIC", Map.of("job", "kafka-exporter", "cluster", "orders"), Map.of("metricPrefix", "kafka_"), OffsetDateTime.now().minusMinutes(1), "CONNECTED", null),
            new DataSourceBinding("bind-kafka-logs", "obj-kafka-orders", "KAFKA", "ds-loki-prod", "LOG", Map.of("app", "kafka", "cluster", "orders"), Map.of("index", "middleware-*"), OffsetDateTime.now().minusMinutes(3), "CONNECTED", null),
            new DataSourceBinding("bind-pg-metrics", "obj-pg-ace", "POSTGRESQL", "ds-prom-prod", "METRIC", Map.of("job", "postgres-exporter", "instance", "ace-pg"), Map.of("metricPrefix", "pg_"), OffsetDateTime.now().minusMinutes(1), "CONNECTED", null),
            new DataSourceBinding("bind-redis-metrics", "obj-redis-cms", "REDIS", "ds-prom-prod", "METRIC", Map.of("job", "redis-exporter", "instance", "cms-redis"), Map.of("metricPrefix", "redis_"), OffsetDateTime.now().minusMinutes(12), "NO_RECENT_DATA", "No samples in the last 10 minutes"),
            new DataSourceBinding("bind-ipro-metrics", "obj-ipro-api", "APPLICATION", "ds-prom-staging", "METRIC", Map.of("job", "ipro-api"), Map.of("metricPrefix", "http_"), OffsetDateTime.now().minusHours(3), "SOURCE_UNAVAILABLE", "Prometheus staging is unavailable")
    );

    private final List<AgentInstance> agents = List.of(
            new AgentInstance("agent-ace-1", "srv-ace-1", "ace-api-01", "prod", "1.8.2", "ONLINE", OffsetDateTime.now().minusSeconds(35), "cfg-20260426-01", null),
            new AgentInstance("agent-ace-2", "srv-ace-2", "ace-api-02", "prod", "1.8.2", "ONLINE", OffsetDateTime.now().minusSeconds(42), "cfg-20260426-01", null),
            new AgentInstance("agent-ipro-1", "srv-ipro-1", "ipro-web-01", "staging", "1.7.0", "CONFIG_ERROR", OffsetDateTime.now().minusMinutes(4), "cfg-20260420-02", "Metric scrape config version is outdated"),
            new AgentInstance("agent-cms-1", "srv-cms-1", "cms-api-01", "prod", "1.8.1", "NO_HEARTBEAT", OffsetDateTime.now().minusMinutes(18), "cfg-20260425-01", "No heartbeat for 18 minutes")
    );

    private final List<MetricDefinition> metricDefinitions = List.of(
            new MetricDefinition("broker_up", "Kafka broker up", "KAFKA", "availability", "count", "PROMETHEUS", "sum(kafka_brokers{cluster=\"$object\"})", List.of("cluster")),
            new MetricDefinition("mq_lag", "MQ consumer lag", "KAFKA", "backlog", "messages", "PROMETHEUS", "sum(kafka_consumergroup_lag{cluster=\"$object\"})", List.of("cluster", "consumer_group")),
            new MetricDefinition("db_conn_usage", "Database connection usage", "POSTGRESQL", "connection", "%", "PROMETHEUS", "pg_stat_activity_count / pg_settings_max_connections * 100", List.of("instance")),
            new MetricDefinition("slow_sql_count", "Slow SQL count", "POSTGRESQL", "sql", "count", "PROMETHEUS", "increase(pg_slow_queries_total{instance=\"$object\"}[5m])", List.of("instance")),
            new MetricDefinition("redis_memory_usage", "Redis memory usage", "REDIS", "capacity", "%", "PROMETHEUS", "redis_memory_used_bytes / redis_memory_max_bytes * 100", List.of("instance")),
            new MetricDefinition("http_5xx_rate", "HTTP 5xx rate", "APPLICATION", "error", "%", "PROMETHEUS", "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[5m]))", List.of("application", "instance"))
    );

    private final List<DefaultMetricMapping> defaultMetricMappings = List.of(
            new DefaultMetricMapping("map-kafka-lag", "KAFKA", "mq_lag", "PROMETHEUS", "kafka_consumergroup_lag", "sum by (cluster, consumergroup) (kafka_consumergroup_lag{cluster=\"$object\"})", "messages", Map.of("cluster", "$object")),
            new DefaultMetricMapping("map-kafka-up", "KAFKA", "broker_up", "PROMETHEUS", "kafka_brokers", "sum(kafka_brokers{cluster=\"$object\"})", "count", Map.of("cluster", "$object")),
            new DefaultMetricMapping("map-pg-conn", "POSTGRESQL", "db_conn_usage", "PROMETHEUS", "pg_stat_activity_count", "pg_stat_activity_count{instance=\"$object\"} / pg_settings_max_connections{instance=\"$object\"} * 100", "%", Map.of("instance", "$object")),
            new DefaultMetricMapping("map-pg-slow", "POSTGRESQL", "slow_sql_count", "PROMETHEUS", "pg_slow_queries_total", "increase(pg_slow_queries_total{instance=\"$object\"}[5m])", "count", Map.of("instance", "$object")),
            new DefaultMetricMapping("map-redis-memory", "REDIS", "redis_memory_usage", "PROMETHEUS", "redis_memory_used_bytes", "redis_memory_used_bytes{instance=\"$object\"} / redis_memory_max_bytes{instance=\"$object\"} * 100", "%", Map.of("instance", "$object"))
    );

    private final List<LogEntry> logEntries = List.of(
            new LogEntry("log-001", OffsetDateTime.now().minusMinutes(12), "app-ace", "obj-pg-ace", "prod", "WARN", "Slow SQL detected: select * from orders where customer_phone='***'", "trace-ace-001", "ds-loki-prod", Map.of("service", "ace-api", "instance", "ace-api-02")),
            new LogEntry("log-002", OffsetDateTime.now().minusMinutes(8), "app-ace", "obj-kafka-orders", "prod", "ERROR", "Consumer lag exceeded threshold for group ace-order-worker", "trace-ace-002", "ds-loki-prod", Map.of("topic", "order-events", "consumerGroup", "ace-order-worker")),
            new LogEntry("log-003", OffsetDateTime.now().minusMinutes(7), "app-ipro", "obj-kafka-orders", "prod", "WARN", "Shared Kafka backlog visible for authorized application summary", "trace-ipro-001", "ds-loki-prod", Map.of("topic", "order-events")),
            new LogEntry("log-004", OffsetDateTime.now().minusMinutes(5), "app-cms", "obj-redis-cms", "prod", "INFO", "Redis memory usage sample delayed", "trace-cms-001", "ds-loki-prod", Map.of("instance", "cms-redis"))
    );

    private final List<AuditEvent> auditEvents = new CopyOnWriteArrayList<>(List.of(
            new AuditEvent("audit-001", "u-admin", "APPLICATION_VIEW", "APPLICATION", "app-ace", "SUCCESS", OffsetDateTime.now().minusHours(3)),
            new AuditEvent("audit-002", "u-sre", "SERVER_LIST", "SERVER", "*", "SUCCESS", OffsetDateTime.now().minusHours(2)),
            new AuditEvent("audit-003", "u-ace-owner", "ME_VIEW", "USER", "u-ace-owner", "SUCCESS", OffsetDateTime.now().minusHours(1))
    ));
    private final AtomicInteger auditSequence = new AtomicInteger(3);
    private final Map<String, Set<String>> applicationGrantsByUser = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> businessLineGrantsByUser = new ConcurrentHashMap<>();

    private final List<RoleInfo> roles = List.of(
            new RoleInfo("r-admin", "PLATFORM_ADMIN", "平台管理员", Set.of("applications:read", "applications:write", "servers:read", "servers:write", "audit:read", "access:read", "access:write", "data-sources:read", "data-sources:write", "agents:read", "metrics:read", "logs:read")),
            new RoleInfo("r-sre", "SRE", "SRE", Set.of("applications:read", "servers:read", "audit:read", "data-sources:read", "data-sources:write", "agents:read", "metrics:read", "logs:read")),
            new RoleInfo("r-app-owner", "APP_OWNER", "应用负责人", Set.of("applications:read", "servers:read", "agents:read", "metrics:read", "logs:read"))
    );

    private final List<UserInfo> users = List.of(
            new UserInfo("u-admin", "platform-admin", "Platform Admin", List.of(roles.get(0)), Set.of(), Set.of("assets", "access", "audit")),
            new UserInfo("u-sre", "sre", "SRE Engineer", List.of(roles.get(1)), Set.of("core-platform"), Set.of("assets", "audit")),
            new UserInfo("u-ace-owner", "ace-owner", "ACE Owner", List.of(roles.get(2)), Set.of("trade"), Set.of("assets"))
    );

    public List<ApplicationAsset> visibleApplications(CurrentUser currentUser) {
        return applications.stream()
                .filter(application -> canAccessApplication(currentUser, application))
                .sorted(Comparator.comparing(ApplicationAsset::id))
                .toList();
    }

    public ApplicationAsset requireVisibleApplication(String id, CurrentUser currentUser) {
        return applications.stream()
                .filter(application -> application.id().equals(id))
                .filter(application -> canAccessApplication(currentUser, application))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Application not found"));
    }

    public synchronized ApplicationAsset saveApplication(ApplicationAsset application, CurrentUser currentUser) {
        requirePermission(currentUser, "applications:write");
        if (!currentUser.dataScope().canAccessEnvironment(application.environment())) {
            throw new ForbiddenException(ErrorCode.ENV_FORBIDDEN, "Environment is outside current user scope");
        }
        String id = application.id() == null || application.id().isBlank()
                ? "app-custom-" + (applications.size() + 1)
                : application.id();
        ApplicationAsset saved = new ApplicationAsset(
                id,
                application.code(),
                application.name(),
                application.businessLine(),
                application.environment(),
                application.ownerUserIds() == null ? List.of() : List.copyOf(application.ownerUserIds()),
                application.accessStatus() == null || application.accessStatus().isBlank() ? "CONNECTED" : application.accessStatus()
        );
        applications.removeIf(item -> item.id().equals(saved.id()));
        applications.add(saved);
        appendAudit(currentUser, "APPLICATION_UPSERT", "APPLICATION", saved.id(), "SUCCESS");
        return saved;
    }

    public synchronized List<ApplicationAsset> importApplications(List<ApplicationAsset> imports, CurrentUser currentUser) {
        requirePermission(currentUser, "applications:write");
        return imports.stream()
                .map(application -> saveApplication(application, currentUser))
                .toList();
    }

    public List<ApplicationInstance> visibleInstances(String applicationId, CurrentUser currentUser) {
        requireVisibleApplication(applicationId, currentUser);
        return instances.stream()
                .filter(instance -> instance.applicationId().equals(applicationId))
                .sorted(Comparator.comparing(ApplicationInstance::id))
                .toList();
    }

    public List<MonitorObject> visibleMonitorObjects(CurrentUser currentUser) {
        Set<String> visibleApplicationIds = visibleApplications(currentUser).stream()
                .map(ApplicationAsset::id)
                .collect(java.util.stream.Collectors.toSet());
        return monitorObjects.stream()
                .filter(object -> object.applicationIds().stream().anyMatch(visibleApplicationIds::contains)
                        || currentUser.dataScope().platformAdmin()
                        || currentUser.dataScope().businessLines().contains(object.businessLine()))
                .filter(object -> currentUser.dataScope().canAccessEnvironment(object.environment()))
                .sorted(Comparator.comparing(MonitorObject::id))
                .toList();
    }

    public MonitorObject requireVisibleMonitorObject(String objectId, CurrentUser currentUser) {
        return visibleMonitorObjects(currentUser).stream()
                .filter(object -> object.id().equals(objectId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Monitor object not found"));
    }

    public List<MonitorObject> visibleApplicationDependencies(String applicationId, CurrentUser currentUser) {
        requireVisibleApplication(applicationId, currentUser);
        return monitorObjects.stream()
                .filter(object -> object.applicationIds().contains(applicationId))
                .filter(object -> currentUser.dataScope().canAccessEnvironment(object.environment()))
                .sorted(Comparator.comparing(MonitorObject::id))
                .toList();
    }

    public List<ServerAsset> visibleServers(CurrentUser currentUser) {
        Set<String> visibleApplicationIds = visibleApplications(currentUser).stream()
                .map(ApplicationAsset::id)
                .collect(java.util.stream.Collectors.toSet());
        return servers.stream()
                .filter(server -> server.applicationIds().stream().anyMatch(visibleApplicationIds::contains))
                .filter(server -> currentUser.dataScope().canAccessEnvironment(server.environment()) || hasExplicitApplicationGrant(currentUser, server.applicationIds()))
                .sorted(Comparator.comparing(ServerAsset::id))
                .toList();
    }

    public synchronized ServerAsset saveServer(ServerAsset server, CurrentUser currentUser) {
        requirePermission(currentUser, "servers:write");
        if (!currentUser.dataScope().canAccessEnvironment(server.environment())) {
            throw new ForbiddenException(ErrorCode.ENV_FORBIDDEN, "Environment is outside current user scope");
        }
        Set<String> applicationIds = server.applicationIds() == null ? Set.of() : Set.copyOf(server.applicationIds());
        applicationIds.forEach(this::requireExistingApplication);
        String id = server.id() == null || server.id().isBlank()
                ? "srv-custom-" + (servers.size() + 1)
                : server.id();
        ServerAsset saved = new ServerAsset(
                id,
                server.hostname(),
                server.ip(),
                server.environment(),
                applicationIds,
                server.accessStatus() == null || server.accessStatus().isBlank() ? "CONNECTED" : server.accessStatus()
        );
        servers.removeIf(item -> item.id().equals(saved.id()));
        servers.add(saved);
        appendAudit(currentUser, "SERVER_UPSERT", "SERVER", saved.id(), "SUCCESS");
        return saved;
    }

    public synchronized List<ServerAsset> importServers(List<ServerAsset> imports, CurrentUser currentUser) {
        requirePermission(currentUser, "servers:write");
        return imports.stream()
                .map(server -> saveServer(server, currentUser))
                .toList();
    }

    public List<DataSourceConfig> visibleDataSources(CurrentUser currentUser) {
        return dataSources.stream()
                .filter(source -> currentUser.dataScope().canAccessEnvironment(source.environment()))
                .sorted(Comparator.comparing(DataSourceConfig::id))
                .toList();
    }

    public DataSourceConfig requireVisibleDataSource(String sourceId, CurrentUser currentUser) {
        return visibleDataSources(currentUser).stream()
                .filter(source -> source.id().equals(sourceId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Data source not found"));
    }

    public DataSourceConfig saveDataSource(DataSourceConfig config, CurrentUser currentUser) {
        if (!currentUser.dataScope().canAccessEnvironment(config.environment())) {
            throw new ForbiddenException(ErrorCode.ENV_FORBIDDEN, "Environment is outside current user scope");
        }
        DataSourceConfig saved = new DataSourceConfig(
                config.id() == null || config.id().isBlank() ? "ds-custom-" + (dataSources.size() + 1) : config.id(),
                config.name(),
                config.type(),
                config.environment(),
                config.baseUrl(),
                config.healthCheckPath(),
                config.authType(),
                config.secretRef(),
                config.timeoutSeconds(),
                config.retryCount(),
                config.rateLimitQps(),
                config.status() == null || config.status().isBlank() ? "DISABLED" : config.status(),
                OffsetDateTime.now(),
                null,
                "VALIDATION_REQUIRED",
                "Data source saved; run validation before enabling production bindings"
        );
        dataSources.removeIf(source -> source.id().equals(saved.id()));
        dataSources.add(saved);
        return saved;
    }

    public List<DataSourceBinding> visibleDataSourceBindings(CurrentUser currentUser, String objectId) {
        Set<String> visibleObjectIds = visibleMonitorObjects(currentUser).stream()
                .map(MonitorObject::id)
                .collect(java.util.stream.Collectors.toSet());
        return dataSourceBindings.stream()
                .filter(binding -> objectId == null || binding.objectId().equals(objectId))
                .filter(binding -> visibleObjectIds.contains(binding.objectId()))
                .sorted(Comparator.comparing(DataSourceBinding::id))
                .toList();
    }

    public DataSourceValidationResult validateDataSource(String sourceId, CurrentUser currentUser) {
        DataSourceConfig source = requireVisibleDataSource(sourceId, currentUser);
        boolean disabled = "DISABLED".equals(source.status());
        boolean unhealthy = "UNHEALTHY".equals(source.status());
        boolean passed = !disabled && !unhealthy;
        List<DataSourceValidationResult.ValidationItem> items = List.of(
                validationItem("basic_config", source.baseUrl() != null && source.baseUrl().startsWith("http"), "CONFIG_INVALID", "Base URL must be an HTTP endpoint"),
                validationItem("connectivity", !unhealthy, Optional.ofNullable(source.lastErrorCode()).orElse("CONNECT_TIMEOUT"), Optional.ofNullable(source.lastErrorMessage()).orElse("Connection test passed")),
                validationItem("auth", source.secretRef() != null && !source.secretRef().isBlank(), "AUTH_FAILED", "Secret reference is configured"),
                validationItem("sample_data", !disabled && !unhealthy, "NO_DATA", "Recent sample data is available")
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

    private static DataSourceValidationResult.ValidationItem validationItem(
            String name,
            boolean passed,
            String errorCode,
            String successMessage
    ) {
        return new DataSourceValidationResult.ValidationItem(
                name,
                passed ? "PASSED" : "FAILED",
                passed ? null : errorCode,
                passed ? successMessage : errorCode
        );
    }

    public List<AgentInstance> visibleAgents(CurrentUser currentUser) {
        Set<String> visibleServerIds = visibleServers(currentUser).stream()
                .map(ServerAsset::id)
                .collect(java.util.stream.Collectors.toSet());
        return agents.stream()
                .filter(agent -> visibleServerIds.contains(agent.serverId()))
                .filter(agent -> currentUser.dataScope().canAccessEnvironment(agent.environment()))
                .sorted(Comparator.comparing(AgentInstance::id))
                .toList();
    }

    public List<MetricDefinition> metricDefinitions(CurrentUser currentUser, String objectType) {
        return metricDefinitions.stream()
                .filter(definition -> objectType == null || definition.objectType().equalsIgnoreCase(objectType))
                .sorted(Comparator.comparing(MetricDefinition::code))
                .toList();
    }

    public List<DefaultMetricMapping> defaultMetricMappings(String objectType) {
        return defaultMetricMappings.stream()
                .filter(mapping -> objectType == null || mapping.objectType().equalsIgnoreCase(objectType))
                .sorted(Comparator.comparing(DefaultMetricMapping::id))
                .toList();
    }

    public MetricSeries queryMetric(CurrentUser currentUser, String metricCode, String objectId, OffsetDateTime from, OffsetDateTime to) {
        MetricDefinition definition = metricDefinitions.stream()
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
        List<MetricSeries.MetricSample> samples = List.of(
                new MetricSeries.MetricSample(start, sampleValue(metricCode, 0)),
                new MetricSeries.MetricSample(start.plusMinutes(10), sampleValue(metricCode, 1)),
                new MetricSeries.MetricSample(start.plusMinutes(20), sampleValue(metricCode, 2)),
                new MetricSeries.MetricSample(end, sampleValue(metricCode, 3))
        );
        return new MetricSeries(metricCode, object.id(), object.name(), definition.unit(), binding.sourceId(), start, end, samples, binding.externalLabels());
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

    public List<LogEntry> searchLogs(CurrentUser currentUser, com.heimdallr.monitor.api.dto.LogSearchCriteria criteria) {
        Set<String> visibleApplicationIds = visibleApplications(currentUser).stream()
                .map(ApplicationAsset::id)
                .collect(java.util.stream.Collectors.toSet());
        return logEntries.stream()
                .filter(log -> visibleApplicationIds.contains(log.applicationId()))
                .filter(log -> criteria.applicationId() == null || log.applicationId().equals(criteria.applicationId()))
                .filter(log -> criteria.objectId() == null || log.objectId().equals(criteria.objectId()))
                .filter(log -> criteria.environment() == null || log.environment().equals(criteria.environment()))
                .filter(log -> currentUser.dataScope().canAccessEnvironment(log.environment()))
                .filter(log -> criteria.level() == null || log.level().equalsIgnoreCase(criteria.level()))
                .filter(log -> criteria.keyword() == null || log.message().toLowerCase().contains(criteria.keyword().toLowerCase()))
                .filter(log -> criteria.traceId() == null || criteria.traceId().equals(log.traceId()))
                .filter(log -> criteria.from() == null || !log.timestamp().isBefore(criteria.from()))
                .filter(log -> criteria.to() == null || !log.timestamp().isAfter(criteria.to()))
                .sorted(Comparator.comparing(LogEntry::timestamp).reversed())
                .toList();
    }

    public List<AuditEvent> auditEvents(CurrentUser currentUser) {
        requirePermission(currentUser, "audit:read");
        return auditEvents.stream()
                .sorted(Comparator.comparing(AuditEvent::occurredAt).thenComparing(AuditEvent::id))
                .toList();
    }

    public List<UserInfo> users(CurrentUser currentUser) {
        requirePermission(currentUser, "access:read");
        return users.stream()
                .map(this::withEffectiveGrants)
                .toList();
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

    public synchronized UserInfo grantApplicationAccess(String userId, String applicationId, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UserInfo user = requireExistingUser(userId);
        requireExistingApplication(applicationId);
        applicationGrantsByUser.computeIfAbsent(user.id(), ignored -> ConcurrentHashMap.newKeySet()).add(applicationId);
        appendAudit(currentUser, "ACCESS_GRANT_APPLICATION", "USER", user.id(), "SUCCESS");
        return withEffectiveGrants(user);
    }

    public synchronized UserInfo revokeApplicationAccess(String userId, String applicationId, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UserInfo user = requireExistingUser(userId);
        requireExistingApplication(applicationId);
        Optional.ofNullable(applicationGrantsByUser.get(user.id())).ifPresent(grants -> grants.remove(applicationId));
        appendAudit(currentUser, "ACCESS_REVOKE_APPLICATION", "USER", user.id(), "SUCCESS");
        return withEffectiveGrants(user);
    }

    public synchronized UserInfo grantBusinessLineAccess(String userId, String businessLine, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UserInfo user = requireExistingUser(userId);
        businessLineGrantsByUser.computeIfAbsent(user.id(), ignored -> ConcurrentHashMap.newKeySet()).add(businessLine);
        appendAudit(currentUser, "ACCESS_GRANT_BUSINESS_LINE", "USER", user.id(), "SUCCESS");
        return withEffectiveGrants(user);
    }

    public synchronized UserInfo revokeBusinessLineAccess(String userId, String businessLine, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UserInfo user = requireExistingUser(userId);
        Optional.ofNullable(businessLineGrantsByUser.get(user.id())).ifPresent(grants -> grants.remove(businessLine));
        appendAudit(currentUser, "ACCESS_REVOKE_BUSINESS_LINE", "USER", user.id(), "SUCCESS");
        return withEffectiveGrants(user);
    }

    private boolean canAccessApplication(CurrentUser currentUser, ApplicationAsset application) {
        return currentUser.dataScope().canAccessApplication(application)
                || Optional.ofNullable(applicationGrantsByUser.get(currentUser.user().id())).orElse(Set.of()).contains(application.id())
                || Optional.ofNullable(businessLineGrantsByUser.get(currentUser.user().id())).orElse(Set.of()).contains(application.businessLine());
    }

    private boolean hasExplicitApplicationGrant(CurrentUser currentUser, Set<String> applicationIds) {
        Set<String> grants = Optional.ofNullable(applicationGrantsByUser.get(currentUser.user().id())).orElse(Set.of());
        return applicationIds.stream().anyMatch(grants::contains);
    }

    private UserInfo requireExistingUser(String userId) {
        return users.stream()
                .filter(user -> user.id().equals(userId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private ApplicationAsset requireExistingApplication(String applicationId) {
        return applications.stream()
                .filter(application -> application.id().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Application not found"));
    }

    private UserInfo withEffectiveGrants(UserInfo user) {
        Set<String> businessLines = new java.util.HashSet<>(user.businessLines());
        businessLines.addAll(Optional.ofNullable(businessLineGrantsByUser.get(user.id())).orElse(Set.of()));
        Set<String> menus = new java.util.HashSet<>(user.menus());
        menus.add("assets");
        return new UserInfo(user.id(), user.username(), user.displayName(), user.roles(), Set.copyOf(businessLines), Set.copyOf(menus));
    }

    private void appendAudit(CurrentUser currentUser, String action, String targetType, String targetId, String result) {
        auditEvents.add(new AuditEvent(
                "audit-" + String.format("%03d", auditSequence.incrementAndGet()),
                currentUser.user().id(),
                action,
                targetType,
                targetId,
                result,
                OffsetDateTime.now()
        ));
    }
}
