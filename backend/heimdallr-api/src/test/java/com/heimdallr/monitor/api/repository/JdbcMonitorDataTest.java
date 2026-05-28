package com.heimdallr.monitor.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JdbcMonitorDataTest {
    @Test
    void triggeredAlertUpsertTargetsActiveDedupPartialIndex() {
        assertThat(JdbcMonitorData.UPSERT_TRIGGERED_ALERT_SQL)
                .contains("ON CONFLICT (dedup_key)")
                .contains("WHERE deleted_at IS NULL AND status NOT IN ('recovered', 'closed')")
                .contains("RETURNING id, (xmax = 0) AS inserted")
                .doesNotContain("ON CONFLICT (id)");
    }
}
