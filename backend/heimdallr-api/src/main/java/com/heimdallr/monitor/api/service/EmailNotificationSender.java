package com.heimdallr.monitor.api.service;

import com.heimdallr.monitor.common.domain.model.AlertEvent;
import com.heimdallr.monitor.common.domain.model.AlertRule;

public interface EmailNotificationSender {
    NotificationDelivery send(AlertRule rule, AlertEvent event, String receiver);

    record NotificationDelivery(boolean success, String failureReason) {
    }
}
