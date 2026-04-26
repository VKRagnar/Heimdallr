package com.datamonitor.common.security;

import com.datamonitor.common.domain.model.DataScope;
import com.datamonitor.common.domain.model.RoleInfo;
import com.datamonitor.common.domain.model.UserInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TokenPrincipalService {
    private static final Set<String> ALL_PERMISSIONS = Set.of(
            "applications:read",
            "servers:read",
            "audit:read",
            "access:read"
    );

    private final Map<String, CurrentUser> usersByToken = Map.of(
            "admin-token",
            user(
                    "u-admin",
                    "platform-admin",
                    "Platform Admin",
                    new RoleInfo("r-admin", "PLATFORM_ADMIN", "平台管理员", ALL_PERMISSIONS),
                    new DataScope(true, Set.of(), Set.of(), Set.of("prod", "staging", "test"))
            ),
            "sre-token",
            user(
                    "u-sre",
                    "sre",
                    "SRE Engineer",
                    new RoleInfo("r-sre", "SRE", "SRE", Set.of("applications:read", "servers:read", "audit:read")),
                    new DataScope(false, Set.of(), Set.of("core-platform"), Set.of("prod", "staging"))
            ),
            "ace-owner-token",
            user(
                    "u-ace-owner",
                    "ace-owner",
                    "ACE Owner",
                    new RoleInfo("r-app-owner", "APP_OWNER", "应用负责人", Set.of("applications:read", "servers:read")),
                    new DataScope(false, Set.of("app-ace"), Set.of("trade"), Set.of("prod"))
            )
    );

    public Optional<CurrentUser> authenticate(String rawAuthorization) {
        String token = normalizeToken(rawAuthorization);
        return Optional.ofNullable(usersByToken.get(token));
    }

    private static CurrentUser user(String id, String username, String displayName, RoleInfo role, DataScope scope) {
        return new CurrentUser(
                new UserInfo(id, username, displayName, List.of(role), scope.businessLines(), Set.of("assets", "access", "audit")),
                scope,
                username
        );
    }

    private static String normalizeToken(String rawAuthorization) {
        if (rawAuthorization == null || rawAuthorization.isBlank()) {
            return "";
        }
        if (rawAuthorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return rawAuthorization.substring(7).trim();
        }
        return rawAuthorization.trim();
    }
}
