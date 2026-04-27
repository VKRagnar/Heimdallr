package com.heimdallr.monitor.api.controller;

import com.heimdallr.monitor.api.dto.LogSearchCriteria;
import com.heimdallr.monitor.api.service.LogQueryService;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.LogEntry;
import com.heimdallr.monitor.common.security.RequestUserContext;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {
    private final LogQueryService service;

    public LogController(LogQueryService service) {
        this.service = service;
    }

    @PostMapping("/search")
    public PageResult<LogEntry> search(@RequestBody LogSearchRequest request) {
        var currentUser = RequestUserContext.requireCurrent();
        return service.search(currentUser, request.toCriteria());
    }

    @GetMapping("/{logId}/context")
    public List<LogEntry> context(@PathVariable("logId") String logId) {
        var currentUser = RequestUserContext.requireCurrent();
        return service.context(currentUser, logId);
    }

    public record LogSearchRequest(
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
        LogSearchCriteria toCriteria() {
            return new LogSearchCriteria(applicationId, objectId, environment, level, keyword, traceId, from, to, pageNo, pageSize);
        }
    }
}
