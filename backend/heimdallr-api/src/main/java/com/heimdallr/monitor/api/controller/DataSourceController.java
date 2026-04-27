package com.heimdallr.monitor.api.controller;

import com.heimdallr.monitor.api.dto.ApplicationAccessStatusResponse;
import com.heimdallr.monitor.api.dto.DataSourceConfigResponse;
import com.heimdallr.monitor.api.service.DataSourceAccessService;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.DataSourceBinding;
import com.heimdallr.monitor.common.domain.model.DataSourceConfig;
import com.heimdallr.monitor.common.domain.model.DataSourceValidationResult;
import com.heimdallr.monitor.common.security.RequestUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data-sources")
public class DataSourceController {
    private final DataSourceAccessService service;

    public DataSourceController(DataSourceAccessService service) {
        this.service = service;
    }

    @GetMapping
    public PageResult<DataSourceConfigResponse> list() {
        var currentUser = RequestUserContext.requireCurrent();
        return service.list(currentUser);
    }

    @GetMapping("/{id}")
    public DataSourceConfigResponse detail(@PathVariable("id") String id) {
        var currentUser = RequestUserContext.requireCurrent();
        return service.detail(id, currentUser);
    }

    @PostMapping
    public DataSourceConfigResponse create(@Valid @RequestBody DataSourceConfigRequest request) {
        var currentUser = RequestUserContext.requireCurrent();
        return service.save(request.toConfig(null), currentUser);
    }

    @PutMapping("/{id}")
    public DataSourceConfigResponse update(@PathVariable("id") String id, @Valid @RequestBody DataSourceConfigRequest request) {
        var currentUser = RequestUserContext.requireCurrent();
        service.detail(id, currentUser);
        return service.save(request.toConfig(id), currentUser);
    }

    @PostMapping("/{id}/validate")
    public DataSourceValidationResult validate(@PathVariable("id") String id) {
        var currentUser = RequestUserContext.requireCurrent();
        return service.validate(id, currentUser);
    }

    @GetMapping("/bindings")
    public PageResult<DataSourceBinding> bindings(@RequestParam(value = "objectId", required = false) String objectId) {
        var currentUser = RequestUserContext.requireCurrent();
        return service.bindings(currentUser, objectId);
    }

    @GetMapping("/access-status")
    public PageResult<ApplicationAccessStatusResponse> accessStatus(@RequestParam(value = "objectId", required = false) String objectId) {
        var currentUser = RequestUserContext.requireCurrent();
        return service.applicationAccess(currentUser, objectId);
    }

    public record DataSourceConfigRequest(
            @NotBlank String name,
            @NotBlank String type,
            @NotBlank String environment,
            @NotBlank String baseUrl,
            String healthCheckPath,
            @NotBlank String authType,
            @NotBlank String secretRef,
            @Min(1) int timeoutSeconds,
            @Min(0) int retryCount,
            @Min(1) int rateLimitQps,
            String status
    ) {
        DataSourceConfig toConfig(String id) {
            return new DataSourceConfig(
                    id,
                    name,
                    type,
                    environment,
                    baseUrl,
                    healthCheckPath,
                    authType,
                    secretRef,
                    timeoutSeconds,
                    retryCount,
                    rateLimitQps,
                    status,
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
