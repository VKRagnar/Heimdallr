package com.heimdallr.monitor.api.adapter;

import com.heimdallr.monitor.api.dto.LogSearchCriteria;
import com.heimdallr.monitor.common.domain.model.LogEntry;
import java.util.List;

public interface LogSourceAdapter {
    List<LogEntry> search(LogSearchCriteria criteria);
}
