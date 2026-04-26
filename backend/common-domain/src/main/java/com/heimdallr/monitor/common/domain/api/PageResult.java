package com.heimdallr.monitor.common.domain.api;

import java.util.List;

public record PageResult<T>(
        List<T> items,
        int pageNo,
        int pageSize,
        long total
) {
    public static <T> PageResult<T> all(List<T> items) {
        return new PageResult<>(items, 1, items.size(), items.size());
    }
}
