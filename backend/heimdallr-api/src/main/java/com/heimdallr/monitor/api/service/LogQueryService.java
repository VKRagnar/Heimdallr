package com.heimdallr.monitor.api.service;

import com.heimdallr.monitor.api.dto.LogSearchCriteria;
import com.heimdallr.monitor.api.repository.MonitorData;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.LogEntry;
import com.heimdallr.monitor.common.security.CurrentUser;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LogQueryService {
    private final MonitorData data;

    public LogQueryService(MonitorData data) {
        this.data = data;
    }

    public PageResult<LogEntry> search(CurrentUser currentUser, LogSearchCriteria criteria) {
        data.requirePermission(currentUser, "logs:read");
        List<LogEntry> filtered = data.searchLogs(currentUser, criteria);
        int pageNo = criteria.normalizedPageNo();
        int pageSize = criteria.normalizedPageSize();
        int fromIndex = Math.min((pageNo - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        return new PageResult<>(filtered.subList(fromIndex, toIndex), pageNo, pageSize, filtered.size());
    }

    public List<LogEntry> context(CurrentUser currentUser, String logId) {
        data.requirePermission(currentUser, "logs:read");
        return data.searchLogs(currentUser, new LogSearchCriteria(null, null, null, null, null, null, null, null, 1, 100)).stream()
                .filter(log -> log.id().equals(logId) || log.traceId().equals(contextTraceId(logId)))
                .toList();
    }

    private static String contextTraceId(String logId) {
        return logId.startsWith("log-") ? "trace-" + logId.substring(4) : logId;
    }
}
