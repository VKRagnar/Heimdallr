package com.heimdallr.monitor.api.service;

import com.heimdallr.monitor.api.repository.MonitorData;
import com.heimdallr.monitor.common.domain.api.ErrorCode;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.exception.ApiException;
import com.heimdallr.monitor.common.domain.exception.NotFoundException;
import com.heimdallr.monitor.common.domain.model.AlertEvent;
import com.heimdallr.monitor.common.domain.model.AlertEventHistory;
import com.heimdallr.monitor.common.domain.model.AlertEvaluationSample;
import com.heimdallr.monitor.common.domain.model.AlertRule;
import com.heimdallr.monitor.common.domain.model.AlertRuleRuntime;
import com.heimdallr.monitor.common.domain.model.MetricSeries;
import com.heimdallr.monitor.common.domain.model.NotificationRecord;
import com.heimdallr.monitor.common.domain.model.OnCallGroup;
import com.heimdallr.monitor.common.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
    private final MonitorData data;
    private final EmailNotificationSender emailSender;

    public AlertService(MonitorData data, EmailNotificationSender emailSender) {
        this.data = data;
        this.emailSender = emailSender;
    }

    public PageResult<AlertRule> rules(CurrentUser currentUser) {
        return PageResult.all(data.alertRules(currentUser));
    }

    public PageResult<AlertRule> rules(CurrentUser currentUser, String severity, String enabled, String keyword) {
        Boolean enabledFilter = enabled == null || enabled.isBlank() ? null : Boolean.parseBoolean(enabled);
        List<AlertRule> filtered = data.alertRules(currentUser).stream()
                .filter(rule -> severity == null || severity.isBlank() || severity.equalsIgnoreCase(rule.severity()))
                .filter(rule -> enabledFilter == null || rule.enabled() == enabledFilter)
                .filter(rule -> matchesKeyword(keyword, rule.name(), rule.objectId(), rule.objectName(), rule.metricCode()))
                .toList();
        return PageResult.all(filtered);
    }

    public AlertRule rule(String ruleId, CurrentUser currentUser) {
        return data.requireAlertRule(ruleId, currentUser);
    }

    public AlertRule saveRule(AlertRule rule, CurrentUser currentUser) {
        validateRule(rule);
        return data.saveAlertRule(rule, currentUser);
    }

    public AlertRule setRuleEnabled(String ruleId, boolean enabled, CurrentUser currentUser) {
        return data.setAlertRuleEnabled(ruleId, enabled, currentUser);
    }

    public AlertRuleRuntime runtime(String ruleId, CurrentUser currentUser) {
        return data.alertRuleRuntime(ruleId, currentUser);
    }

    public PageResult<AlertEvaluationSample> samples(String ruleId, CurrentUser currentUser) {
        return PageResult.all(data.alertEvaluationSamples(ruleId, currentUser));
    }

    public AlertEvent evaluateRule(String ruleId, CurrentUser currentUser) {
        data.requirePermission(currentUser, "alerts:write");
        AlertRule rule = data.requireAlertRule(ruleId, currentUser);
        if (!rule.enabled()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, 400, "Alert rule is disabled");
        }
        long startedAt = System.nanoTime();
        boolean[] recorded = {false};
        OffsetDateTime to = OffsetDateTime.now();
        OffsetDateTime from = to.minusSeconds(Math.max(rule.windowSeconds(), rule.evaluationIntervalSeconds()));
        try {
            MetricSeries series = data.queryMetric(currentUser, rule.metricCode(), rule.objectId(), from, to);
            MetricSeries.MetricSample latest = series.samples().stream()
                    .max(Comparator.comparing(MetricSeries.MetricSample::timestamp))
                    .orElse(null);
            if (latest == null) {
                data.recordAlertEvaluation(rule, "no_data", null, false, null, "Metric has no samples", elapsedMillis(startedAt), currentUser);
                recorded[0] = true;
                throw new ApiException(ErrorCode.METRIC_NO_RECENT_DATA, 409, "Metric has no samples");
            }
            if (matches(rule.operator(), latest.value(), rule.threshold())) {
                AlertEvent event = data.upsertTriggeredAlert(rule, latest.value(), currentUser);
                AlertEvent evaluatedEvent = event;
                if ("TRIGGERED".equalsIgnoreCase(event.status())) {
                    notifyFirstReceiver(rule, event, currentUser);
                    evaluatedEvent = data.alertEvents(currentUser, null).stream()
                            .filter(item -> item.id().equals(event.id()))
                            .findFirst()
                            .orElse(event);
                }
                data.recordAlertEvaluation(rule, "matched", latest.value(), true, evaluatedEvent.id(), null, elapsedMillis(startedAt), currentUser);
                recorded[0] = true;
                return evaluatedEvent;
            }
            try {
                AlertEvent recovered = data.recoverActiveAlert(rule, latest.value(), currentUser);
                data.recordAlertEvaluation(rule, "recovered", latest.value(), false, recovered.id(), null, elapsedMillis(startedAt), currentUser);
                recorded[0] = true;
                return recovered;
            } catch (NotFoundException ignored) {
                data.recordAlertEvaluation(rule, "normal", latest.value(), false, null, null, elapsedMillis(startedAt), currentUser);
                recorded[0] = true;
                throw new ApiException(ErrorCode.METRIC_NO_RECENT_DATA, 409, "No active alert and threshold condition is not met");
            }
        } catch (ApiException ex) {
            if (!recorded[0]) {
                String status = ex.code() == ErrorCode.METRIC_NO_RECENT_DATA ? "no_data" : "failed";
                data.recordAlertEvaluation(rule, status, null, false, null, ex.getMessage(), elapsedMillis(startedAt), currentUser);
            }
            throw ex;
        } catch (RuntimeException ex) {
            if (!recorded[0]) {
                data.recordAlertEvaluation(rule, "failed", null, false, null, ex.getMessage(), elapsedMillis(startedAt), currentUser);
            }
            throw ex;
        }
    }

    public PageResult<AlertEvent> evaluateEnabledRules(CurrentUser currentUser) {
        data.requirePermission(currentUser, "alerts:write");
        List<AlertEvent> events = new ArrayList<>();
        for (AlertRule rule : data.alertRules(currentUser)) {
            if (!rule.enabled()) {
                continue;
            }
            try {
                events.add(evaluateRule(rule.id(), currentUser));
            } catch (ApiException ex) {
                if (ex.httpStatus() != 409) {
                    throw ex;
                }
            }
        }
        return PageResult.all(events);
    }

    public PageResult<AlertEvent> evaluateDueRules(CurrentUser currentUser, int limit) {
        data.requirePermission(currentUser, "alerts:write");
        List<AlertEvent> events = new ArrayList<>();
        for (AlertRule rule : data.dueAlertRules(currentUser, OffsetDateTime.now(), limit)) {
            try {
                events.add(evaluateRule(rule.id(), currentUser));
            } catch (ApiException ex) {
                if (ex.httpStatus() != 409) {
                    throw ex;
                }
            }
        }
        return PageResult.all(events);
    }

    public PageResult<AlertEvent> events(CurrentUser currentUser, String status) {
        return PageResult.all(data.alertEvents(currentUser, status));
    }

    public PageResult<AlertEvent> events(CurrentUser currentUser, String status, String severity, String keyword) {
        List<AlertEvent> filtered = data.alertEvents(currentUser, status).stream()
                .filter(event -> severity == null || severity.isBlank() || severity.equalsIgnoreCase(event.severity()))
                .filter(event -> matchesKeyword(keyword, event.ruleName(), event.objectId(), event.objectName(), event.metricCode()))
                .toList();
        return PageResult.all(filtered);
    }

    public AlertEvent transitionEvent(String eventId, String action, String message, CurrentUser currentUser) {
        return data.transitionAlertEvent(eventId, action, message, currentUser);
    }

    public PageResult<AlertEventHistory> history(String eventId, CurrentUser currentUser) {
        return PageResult.all(data.alertEventHistory(eventId, currentUser));
    }

    public PageResult<NotificationRecord> notifications(CurrentUser currentUser, String eventId) {
        return PageResult.all(data.notificationRecords(currentUser, eventId));
    }

    public PageResult<NotificationRecord> retryDueNotifications(CurrentUser currentUser, int limit) {
        return retryDueNotifications(currentUser, OffsetDateTime.now(), limit);
    }

    PageResult<NotificationRecord> retryDueNotifications(CurrentUser currentUser, OffsetDateTime now, int limit) {
        data.requirePermission(currentUser, "alerts:write");
        List<NotificationRecord> retried = new ArrayList<>();
        for (NotificationRecord record : data.dueNotificationRetries(currentUser, now, limit)) {
            AlertRule rule = data.requireAlertRule(record.ruleId(), currentUser);
            AlertEvent event = data.alertEvents(currentUser, null).stream()
                    .filter(item -> item.id().equals(record.eventId()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Alert event not found"));
            EmailNotificationSender.NotificationDelivery delivery = emailSender.send(rule, event, record.receiver());
            retried.add(data.recordNotificationRetry(record.id(), delivery.success(), delivery.failureReason(), currentUser));
        }
        return PageResult.all(retried);
    }

    public PageResult<OnCallGroup> onCallGroups(CurrentUser currentUser) {
        return PageResult.all(data.onCallGroups(currentUser));
    }

    private void notifyFirstReceiver(AlertRule rule, AlertEvent event, CurrentUser currentUser) {
        String receiver = data.onCallGroups(currentUser).stream()
                .filter(group -> group.id().equals(rule.onCallGroupId()) || group.code().equals(rule.onCallGroupId()))
                .flatMap(group -> group.memberUserIds().stream())
                .findFirst()
                .orElse(currentUser.user().id());
        EmailNotificationSender.NotificationDelivery delivery = emailSender.send(rule, event, receiver);
        data.recordNotification(event.id(), rule.id(), receiver, delivery.success(), delivery.failureReason(), currentUser);
    }

    private static void validateRule(AlertRule rule) {
        if (rule.durationSeconds() > 0 && rule.durationSeconds() < rule.evaluationIntervalSeconds()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, 400, "durationSeconds must be greater than or equal to evaluationIntervalSeconds");
        }
        if (!java.util.Set.of(">", ">=", "<", "<=", "=", "!=").contains(rule.operator())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, 400, "Unsupported alert operator");
        }
    }

    private static boolean matches(String operator, double value, double threshold) {
        return switch (operator) {
            case ">" -> value > threshold;
            case ">=" -> value >= threshold;
            case "<" -> value < threshold;
            case "<=" -> value <= threshold;
            case "=" -> Double.compare(value, threshold) == 0;
            case "!=" -> Double.compare(value, threshold) != 0;
            default -> false;
        };
    }

    private static long elapsedMillis(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private static boolean matchesKeyword(String keyword, String... values) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.toLowerCase();
        for (String value : values) {
            if (value != null && value.toLowerCase().contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
