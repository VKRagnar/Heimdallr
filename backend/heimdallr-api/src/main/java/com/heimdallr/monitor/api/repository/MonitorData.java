package com.heimdallr.monitor.api.repository;

import com.heimdallr.monitor.api.dto.LogSearchCriteria;
import com.heimdallr.monitor.common.domain.model.AgentInstance;
import com.heimdallr.monitor.common.domain.model.AlertEvent;
import com.heimdallr.monitor.common.domain.model.AlertEventHistory;
import com.heimdallr.monitor.common.domain.model.AlertEvaluationSample;
import com.heimdallr.monitor.common.domain.model.AlertRule;
import com.heimdallr.monitor.common.domain.model.AlertRuleRuntime;
import com.heimdallr.monitor.common.domain.model.ApplicationAsset;
import com.heimdallr.monitor.common.domain.model.ApplicationInstance;
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
import java.time.OffsetDateTime;
import java.util.List;

public interface MonitorData {
    List<ApplicationAsset> visibleApplications(CurrentUser currentUser);

    ApplicationAsset requireVisibleApplication(String id, CurrentUser currentUser);

    ApplicationAsset saveApplication(ApplicationAsset application, CurrentUser currentUser);

    List<ApplicationAsset> importApplications(List<ApplicationAsset> imports, CurrentUser currentUser);

    List<ApplicationInstance> visibleInstances(String applicationId, CurrentUser currentUser);

    List<MonitorObject> visibleMonitorObjects(CurrentUser currentUser);

    MonitorObject requireVisibleMonitorObject(String objectId, CurrentUser currentUser);

    List<MonitorObject> visibleApplicationDependencies(String applicationId, CurrentUser currentUser);

    List<ServerAsset> visibleServers(CurrentUser currentUser);

    ServerAsset saveServer(ServerAsset server, CurrentUser currentUser);

    List<ServerAsset> importServers(List<ServerAsset> imports, CurrentUser currentUser);

    List<DataSourceConfig> visibleDataSources(CurrentUser currentUser);

    DataSourceConfig requireVisibleDataSource(String sourceId, CurrentUser currentUser);

    DataSourceConfig saveDataSource(DataSourceConfig config, CurrentUser currentUser);

    List<DataSourceBinding> visibleDataSourceBindings(CurrentUser currentUser, String objectId);

    DataSourceValidationResult validateDataSource(String sourceId, CurrentUser currentUser);

    List<AgentInstance> visibleAgents(CurrentUser currentUser);

    List<MetricDefinition> metricDefinitions(CurrentUser currentUser, String objectType);

    List<DefaultMetricMapping> defaultMetricMappings(String objectType);

    MetricSeries queryMetric(CurrentUser currentUser, String metricCode, String objectId, OffsetDateTime from, OffsetDateTime to);

    List<AlertRule> alertRules(CurrentUser currentUser);

    List<AlertRule> dueAlertRules(CurrentUser currentUser, OffsetDateTime now, int limit);

    AlertRule requireAlertRule(String ruleId, CurrentUser currentUser);

    AlertRule saveAlertRule(AlertRule rule, CurrentUser currentUser);

    AlertRule setAlertRuleEnabled(String ruleId, boolean enabled, CurrentUser currentUser);

    AlertRuleRuntime alertRuleRuntime(String ruleId, CurrentUser currentUser);

    List<AlertEvaluationSample> alertEvaluationSamples(String ruleId, CurrentUser currentUser);

    AlertRuleRuntime recordAlertEvaluation(AlertRule rule, String status, Double value, boolean matched, String eventId, String error, long evaluationDurationMs, CurrentUser currentUser);

    List<AlertEvent> alertEvents(CurrentUser currentUser, String status);

    AlertEvent upsertTriggeredAlert(AlertRule rule, double triggerValue, CurrentUser currentUser);

    AlertEvent recoverActiveAlert(AlertRule rule, double latestValue, CurrentUser currentUser);

    AlertEvent transitionAlertEvent(String eventId, String action, String message, CurrentUser currentUser);

    List<AlertEventHistory> alertEventHistory(String eventId, CurrentUser currentUser);

    List<NotificationRecord> notificationRecords(CurrentUser currentUser, String eventId);

    NotificationRecord recordNotification(String eventId, String ruleId, String receiver, boolean success, String failureReason, CurrentUser currentUser);

    List<NotificationRecord> dueNotificationRetries(CurrentUser currentUser, OffsetDateTime now, int limit);

    NotificationRecord recordNotificationRetry(String notificationId, boolean success, String failureReason, CurrentUser currentUser);

    List<OnCallGroup> onCallGroups(CurrentUser currentUser);

    List<LogEntry> searchLogs(CurrentUser currentUser, LogSearchCriteria criteria);

    List<AuditEvent> auditEvents(CurrentUser currentUser);

    List<UserInfo> users(CurrentUser currentUser);

    List<RoleInfo> roles(CurrentUser currentUser);

    void requirePermission(CurrentUser currentUser, String permission);

    UserInfo grantApplicationAccess(String userId, String applicationId, CurrentUser currentUser);

    UserInfo revokeApplicationAccess(String userId, String applicationId, CurrentUser currentUser);

    UserInfo grantBusinessLineAccess(String userId, String businessLine, CurrentUser currentUser);

    UserInfo revokeBusinessLineAccess(String userId, String businessLine, CurrentUser currentUser);
}
