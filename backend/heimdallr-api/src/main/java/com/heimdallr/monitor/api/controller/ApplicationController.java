package com.heimdallr.monitor.api.controller;

import com.heimdallr.monitor.api.repository.MonitorData;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.ApplicationAsset;
import com.heimdallr.monitor.common.domain.model.ApplicationInstance;
import com.heimdallr.monitor.common.domain.model.MonitorObject;
import com.heimdallr.monitor.common.security.RequestUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {
    private final MonitorData data;

    public ApplicationController(MonitorData data) {
        this.data = data;
    }

    @GetMapping
    public PageResult<ApplicationAsset> list() {
        var currentUser = RequestUserContext.requireCurrent();
        data.requirePermission(currentUser, "applications:read");
        return PageResult.all(data.visibleApplications(currentUser));
    }

    @GetMapping("/{id}")
    public ApplicationAsset detail(@PathVariable("id") String id) {
        var currentUser = RequestUserContext.requireCurrent();
        data.requirePermission(currentUser, "applications:read");
        return data.requireVisibleApplication(id, currentUser);
    }

    @PostMapping
    public ApplicationAsset create(@Valid @RequestBody ApplicationAssetRequest request) {
        var currentUser = RequestUserContext.requireCurrent();
        return data.saveApplication(request.toAsset(request.id()), currentUser);
    }

    @PutMapping("/{id}")
    public ApplicationAsset update(@PathVariable("id") String id, @Valid @RequestBody ApplicationAssetRequest request) {
        var currentUser = RequestUserContext.requireCurrent();
        return data.saveApplication(request.toAsset(id), currentUser);
    }

    @PostMapping("/import")
    public PageResult<ApplicationAsset> importApplications(@NotEmpty @RequestBody List<@Valid ApplicationAssetRequest> request) {
        var currentUser = RequestUserContext.requireCurrent();
        return PageResult.all(data.importApplications(request.stream()
                .map(item -> item.toAsset(item.id()))
                .toList(), currentUser));
    }

    @GetMapping("/{id}/instances")
    public List<ApplicationInstance> instances(@PathVariable("id") String id) {
        var currentUser = RequestUserContext.requireCurrent();
        data.requirePermission(currentUser, "applications:read");
        return data.visibleInstances(id, currentUser);
    }

    @GetMapping("/{id}/dependencies")
    public List<MonitorObject> dependencies(@PathVariable("id") String id) {
        var currentUser = RequestUserContext.requireCurrent();
        data.requirePermission(currentUser, "applications:read");
        return data.visibleApplicationDependencies(id, currentUser);
    }

    public record ApplicationAssetRequest(
            String id,
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String businessLine,
            @NotBlank String environment,
            List<String> ownerUserIds,
            String accessStatus
    ) {
        ApplicationAsset toAsset(String assetId) {
            return new ApplicationAsset(assetId, code, name, businessLine, environment, ownerUserIds, accessStatus);
        }
    }
}
