package com.heimdallr.monitor.api.controller;

import com.heimdallr.monitor.api.service.MetricQueryService;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.DefaultMetricMapping;
import com.heimdallr.monitor.common.domain.model.MetricDefinition;
import com.heimdallr.monitor.common.domain.model.MetricSeries;
import com.heimdallr.monitor.common.security.RequestUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricController {
    private final MetricQueryService service;

    public MetricController(MetricQueryService service) {
        this.service = service;
    }

    @GetMapping("/definitions")
    public PageResult<MetricDefinition> definitions(@RequestParam(value = "objectType", required = false) String objectType) {
        var currentUser = RequestUserContext.requireCurrent();
        return service.definitions(currentUser, objectType);
    }

    @GetMapping("/default-mappings")
    public PageResult<DefaultMetricMapping> defaultMappings(@RequestParam(value = "objectType", required = false) String objectType) {
        var currentUser = RequestUserContext.requireCurrent();
        return service.defaultMappings(currentUser, objectType);
    }

    @PostMapping("/query")
    public MetricSeries query(@Valid @RequestBody MetricQueryRequest request) {
        var currentUser = RequestUserContext.requireCurrent();
        return service.query(currentUser, request.metricCode(), request.objectId(), request.from(), request.to());
    }

    public record MetricQueryRequest(
            @NotBlank String metricCode,
            @NotBlank String objectId,
            OffsetDateTime from,
            OffsetDateTime to,
            String step
    ) {
    }
}
