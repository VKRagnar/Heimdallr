package com.heimdallr.monitor.api.controller;

import com.heimdallr.monitor.api.service.AlertService;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.AlertEvent;
import com.heimdallr.monitor.common.domain.model.AlertEventHistory;
import com.heimdallr.monitor.common.domain.model.AlertEvaluationSample;
import com.heimdallr.monitor.common.domain.model.AlertRule;
import com.heimdallr.monitor.common.domain.model.AlertRuleRuntime;
import com.heimdallr.monitor.common.domain.model.NotificationRecord;
import com.heimdallr.monitor.common.domain.model.OnCallGroup;
import com.heimdallr.monitor.common.security.RequestUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {
    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping("/rules")
    public PageResult<AlertRule> rules(
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "enabled", required = false) String enabled,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return service.rules(RequestUserContext.requireCurrent(), severity, enabled, keyword);
    }

    @GetMapping("/rules/{id}")
    public AlertRule rule(@PathVariable("id") String id) {
        return service.rule(id, RequestUserContext.requireCurrent());
    }

    @PostMapping("/rules")
    public AlertRule createRule(@Valid @RequestBody AlertRuleRequest request) {
        return service.saveRule(request.toRule(null), RequestUserContext.requireCurrent());
    }

    @PutMapping("/rules/{id}")
    public AlertRule updateRule(@PathVariable("id") String id, @Valid @RequestBody AlertRuleRequest request) {
        return service.saveRule(request.toRule(id), RequestUserContext.requireCurrent());
    }

    @PostMapping("/rules/{id}/enable")
    public AlertRule enableRule(@PathVariable("id") String id) {
        return service.setRuleEnabled(id, true, RequestUserContext.requireCurrent());
    }

    @PostMapping("/rules/{id}/disable")
    public AlertRule disableRule(@PathVariable("id") String id) {
        return service.setRuleEnabled(id, false, RequestUserContext.requireCurrent());
    }

    @PostMapping("/rules/{id}/evaluate")
    public AlertEvent evaluateRule(@PathVariable("id") String id) {
        return service.evaluateRule(id, RequestUserContext.requireCurrent());
    }

    @GetMapping("/rules/{id}/runtime")
    public AlertRuleRuntime runtime(@PathVariable("id") String id) {
        return service.runtime(id, RequestUserContext.requireCurrent());
    }

    @GetMapping("/rules/{id}/samples")
    public PageResult<AlertEvaluationSample> samples(@PathVariable("id") String id) {
        return service.samples(id, RequestUserContext.requireCurrent());
    }

    @PostMapping("/rules/evaluate")
    public PageResult<AlertEvent> evaluateEnabledRules() {
        return service.evaluateEnabledRules(RequestUserContext.requireCurrent());
    }

    @PostMapping("/rules/evaluate-due")
    public PageResult<AlertEvent> evaluateDueRules(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        return service.evaluateDueRules(RequestUserContext.requireCurrent(), limit);
    }

    @GetMapping("/events")
    public PageResult<AlertEvent> events(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return service.events(RequestUserContext.requireCurrent(), status, severity, keyword);
    }

    @PostMapping("/events/{id}/actions")
    public AlertEvent transition(@PathVariable("id") String id, @Valid @RequestBody AlertEventActionRequest request) {
        return service.transitionEvent(id, request.action(), request.message(), RequestUserContext.requireCurrent());
    }

    @GetMapping("/events/{id}/history")
    public PageResult<AlertEventHistory> history(@PathVariable("id") String id) {
        return service.history(id, RequestUserContext.requireCurrent());
    }

    @GetMapping("/notifications")
    public PageResult<NotificationRecord> notifications(@RequestParam(value = "eventId", required = false) String eventId) {
        return service.notifications(RequestUserContext.requireCurrent(), eventId);
    }

    @PostMapping("/notifications/retry-due")
    public PageResult<NotificationRecord> retryDueNotifications(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        return service.retryDueNotifications(RequestUserContext.requireCurrent(), limit);
    }

    @GetMapping("/on-call-groups")
    public PageResult<OnCallGroup> onCallGroups() {
        return service.onCallGroups(RequestUserContext.requireCurrent());
    }

    public record AlertRuleRequest(
            @NotBlank String name,
            @NotBlank String objectId,
            @NotBlank String metricCode,
            @NotBlank String operator,
            double threshold,
            @Min(1) int windowSeconds,
            @Min(0) int durationSeconds,
            @Min(1) int evaluationIntervalSeconds,
            String severity,
            boolean enabled,
            String onCallGroupId
    ) {
        AlertRule toRule(String id) {
            return new AlertRule(
                    id,
                    name,
                    objectId,
                    null,
                    metricCode,
                    operator,
                    threshold,
                    windowSeconds,
                    durationSeconds,
                    evaluationIntervalSeconds,
                    severity,
                    enabled,
                    null,
                    null,
                    onCallGroupId,
                    null,
                    null
            );
        }
    }

    public record AlertEventActionRequest(
            @NotBlank String action,
            String message
    ) {
    }
}
