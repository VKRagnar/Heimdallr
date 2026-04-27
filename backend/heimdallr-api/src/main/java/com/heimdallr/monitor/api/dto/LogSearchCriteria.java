package com.heimdallr.monitor.api.dto;

import java.time.OffsetDateTime;

public record LogSearchCriteria(
        String applicationId,
        String objectId,
        String environment,
        String level,
        String keyword,
        String traceId,
        OffsetDateTime from,
        OffsetDateTime to,
        Integer pageNo,
        Integer pageSize
) {
    public int normalizedPageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public int normalizedPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }
}
