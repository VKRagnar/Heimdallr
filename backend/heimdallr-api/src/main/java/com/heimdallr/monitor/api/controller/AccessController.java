package com.heimdallr.monitor.api.controller;

import com.heimdallr.monitor.api.repository.MonitorData;
import com.heimdallr.monitor.common.domain.api.PageResult;
import com.heimdallr.monitor.common.domain.model.RoleInfo;
import com.heimdallr.monitor.common.domain.model.UserInfo;
import com.heimdallr.monitor.common.security.RequestUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/access")
public class AccessController {
    private final MonitorData data;

    public AccessController(MonitorData data) {
        this.data = data;
    }

    @GetMapping("/users")
    public PageResult<UserInfo> users() {
        return PageResult.all(data.users(RequestUserContext.requireCurrent()));
    }

    @GetMapping("/roles")
    public PageResult<RoleInfo> roles() {
        return PageResult.all(data.roles(RequestUserContext.requireCurrent()));
    }

    @PostMapping("/users/{userId}/applications")
    public UserInfo grantApplication(@PathVariable("userId") String userId, @Valid @RequestBody ApplicationGrantRequest request) {
        return data.grantApplicationAccess(userId, request.applicationId(), RequestUserContext.requireCurrent());
    }

    @DeleteMapping("/users/{userId}/applications/{applicationId}")
    public UserInfo revokeApplication(@PathVariable("userId") String userId, @PathVariable("applicationId") String applicationId) {
        return data.revokeApplicationAccess(userId, applicationId, RequestUserContext.requireCurrent());
    }

    @PostMapping("/users/{userId}/business-lines")
    public UserInfo grantBusinessLine(@PathVariable("userId") String userId, @Valid @RequestBody BusinessLineGrantRequest request) {
        return data.grantBusinessLineAccess(userId, request.businessLine(), RequestUserContext.requireCurrent());
    }

    @DeleteMapping("/users/{userId}/business-lines/{businessLine}")
    public UserInfo revokeBusinessLine(@PathVariable("userId") String userId, @PathVariable("businessLine") String businessLine) {
        return data.revokeBusinessLineAccess(userId, businessLine, RequestUserContext.requireCurrent());
    }

    public record ApplicationGrantRequest(@NotBlank String applicationId) {
    }

    public record BusinessLineGrantRequest(@NotBlank String businessLine) {
    }
}
