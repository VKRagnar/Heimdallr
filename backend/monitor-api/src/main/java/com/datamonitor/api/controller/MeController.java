package com.datamonitor.api.controller;

import com.datamonitor.common.domain.model.DataScope;
import com.datamonitor.common.domain.model.UserInfo;
import com.datamonitor.common.security.RequestUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    @GetMapping
    public UserInfo me() {
        return RequestUserContext.requireCurrent().user();
    }

    @GetMapping("/data-scope")
    public DataScope dataScope() {
        return RequestUserContext.requireCurrent().dataScope();
    }
}
