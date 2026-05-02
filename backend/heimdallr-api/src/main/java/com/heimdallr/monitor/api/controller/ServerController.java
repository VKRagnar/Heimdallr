package com.heimdallr.monitor.api.controller;

import com.heimdallr.monitor.api.repository.MonitorData;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.ServerAsset;
import com.heimdallr.monitor.common.security.RequestUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/servers")
public class ServerController {
    private final MonitorData data;

    public ServerController(MonitorData data) {
        this.data = data;
    }

    @GetMapping
    public PageResult<ServerAsset> list() {
        var currentUser = RequestUserContext.requireCurrent();
        data.requirePermission(currentUser, "servers:read");
        return PageResult.all(data.visibleServers(currentUser));
    }

    @PostMapping
    public ServerAsset create(@Valid @RequestBody ServerAssetRequest request) {
        var currentUser = RequestUserContext.requireCurrent();
        return data.saveServer(request.toAsset(request.id()), currentUser);
    }

    @PutMapping("/{id}")
    public ServerAsset update(@PathVariable("id") String id, @Valid @RequestBody ServerAssetRequest request) {
        var currentUser = RequestUserContext.requireCurrent();
        return data.saveServer(request.toAsset(id), currentUser);
    }

    @PostMapping("/import")
    public PageResult<ServerAsset> importServers(@NotEmpty @RequestBody List<@Valid ServerAssetRequest> request) {
        var currentUser = RequestUserContext.requireCurrent();
        return PageResult.all(data.importServers(request.stream()
                .map(item -> item.toAsset(item.id()))
                .toList(), currentUser));
    }

    public record ServerAssetRequest(
            String id,
            @NotBlank String hostname,
            @NotBlank String ip,
            @NotBlank String environment,
            @NotEmpty Set<String> applicationIds,
            String accessStatus
    ) {
        ServerAsset toAsset(String assetId) {
            return new ServerAsset(assetId, hostname, ip, environment, applicationIds, accessStatus);
        }
    }
}
