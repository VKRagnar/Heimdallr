package com.heimdallr.monitor.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.heimdallr.monitor.api.fixture.InMemoryMonitorData;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.AlertEvent;
import com.heimdallr.monitor.common.domain.model.AlertRule;
import com.heimdallr.monitor.common.domain.model.NotificationRecord;
import com.heimdallr.monitor.common.security.CurrentUser;
import com.heimdallr.monitor.common.security.TokenPrincipalService;
import org.junit.jupiter.api.Test;

class AlertServiceTest {
    @Test
    void retryDueNotificationsResendsFailedRecordAndMarksEventNotified() {
        InMemoryMonitorData data = new InMemoryMonitorData();
        CurrentUser admin = new TokenPrincipalService().authenticate("admin-token").orElseThrow();
        AlertRule rule = data.saveAlertRule(kafkaRule("Kafka retry smoke"), admin);
        AlertEvent event = data.upsertTriggeredAlert(rule, 1280, admin);
        NotificationRecord failed = data.recordNotification(event.id(), rule.id(), "u-ace-owner", false, "SMTP timeout", admin);
        AlertService service = new AlertService(
                data,
                (sentRule, sentEvent, receiver) -> new EmailNotificationSender.NotificationDelivery(true, null)
        );

        PageResult<NotificationRecord> retried = service.retryDueNotifications(admin, failed.nextRetryAt().plusSeconds(1), 10);

        assertThat(retried.total()).isEqualTo(1);
        NotificationRecord updated = retried.items().getFirst();
        assertThat(updated.id()).isEqualTo(failed.id());
        assertThat(updated.status()).isEqualTo("SENT");
        assertThat(updated.retryCount()).isEqualTo(2);
        assertThat(updated.nextRetryAt()).isNull();
        assertThat(data.alertEvents(admin, null))
                .filteredOn(item -> item.id().equals(event.id()))
                .singleElement()
                .extracting(AlertEvent::status)
                .isEqualTo("NOTIFIED");
    }

    private static AlertRule kafkaRule(String name) {
        return new AlertRule(
                null,
                name,
                "obj-kafka-orders",
                null,
                "mq_lag",
                ">",
                1000,
                300,
                60,
                60,
                "P1",
                true,
                null,
                null,
                "trade-oncall",
                null,
                null
        );
    }
}
