package com.heimdallr.monitor.api.service;

import com.heimdallr.monitor.common.domain.model.AlertEvent;
import com.heimdallr.monitor.common.domain.model.AlertRule;
import org.springframework.stereotype.Component;

@Component
public class MockEmailNotificationSender implements EmailNotificationSender {
    @Override
    public NotificationDelivery send(AlertRule rule, AlertEvent event, String receiver) {
        if (receiver == null || receiver.isBlank()) {
            return new NotificationDelivery(false, "Receiver is empty");
        }
        return new NotificationDelivery(true, null);
    }
}
