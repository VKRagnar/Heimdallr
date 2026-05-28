package com.heimdallr.monitor.api.service;

import com.heimdallr.monitor.common.domain.api.ErrorCode;
import com.heimdallr.monitor.common.domain.exception.ApiException;
import com.heimdallr.monitor.common.domain.model.DataScope;
import com.heimdallr.monitor.common.domain.model.RoleInfo;
import com.heimdallr.monitor.common.domain.model.UserInfo;
import com.heimdallr.monitor.common.security.CurrentUser;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlertEvaluationJob {
    private static final Logger log = LoggerFactory.getLogger(AlertEvaluationJob.class);
    private static final Set<String> ALERT_PERMISSIONS = Set.of("alerts:read", "alerts:write", "metrics:read");
    private static final CurrentUser SYSTEM_USER = new CurrentUser(
            new UserInfo(
                    "system-alert-scheduler",
                    "alert-scheduler",
                    "Alert Scheduler",
                    List.of(new RoleInfo("system-alert-runtime", "SYSTEM", "System Alert Runtime", ALERT_PERMISSIONS)),
                    Set.of(),
                    Set.of("alerts")
            ),
            new DataScope(true, Set.of(), Set.of(), Set.of("prod", "staging", "test", "dev")),
            "alert-scheduler"
    );

    private final AlertService alertService;

    public AlertEvaluationJob(AlertService alertService) {
        this.alertService = alertService;
    }

    @Scheduled(
            fixedDelayString = "${heimdallr.alert.scheduler.fixed-delay-ms:60000}",
            initialDelayString = "${heimdallr.alert.scheduler.initial-delay-ms:60000}"
    )
    public void scanDueRules() {
        try {
            alertService.evaluateDueRules(SYSTEM_USER, 100);
        } catch (ApiException ex) {
            if (ex.code() != ErrorCode.METRIC_NO_RECENT_DATA) {
                log.warn("Alert due-rule scan failed: {}", ex.getMessage());
            }
        } catch (RuntimeException ex) {
            log.warn("Alert due-rule scan failed unexpectedly: {}", ex.getMessage());
        }
    }

    @Scheduled(
            fixedDelayString = "${heimdallr.alert.notification-retry.fixed-delay-ms:60000}",
            initialDelayString = "${heimdallr.alert.notification-retry.initial-delay-ms:90000}"
    )
    public void retryDueNotifications() {
        try {
            alertService.retryDueNotifications(SYSTEM_USER, 50);
        } catch (ApiException ex) {
            log.warn("Alert notification retry failed: {}", ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Alert notification retry failed unexpectedly: {}", ex.getMessage());
        }
    }
}
