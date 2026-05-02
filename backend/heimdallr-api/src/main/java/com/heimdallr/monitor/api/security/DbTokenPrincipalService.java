package com.heimdallr.monitor.api.security;

import com.heimdallr.monitor.common.domain.model.DataScope;
import com.heimdallr.monitor.common.domain.model.RoleInfo;
import com.heimdallr.monitor.common.domain.model.UserInfo;
import com.heimdallr.monitor.common.security.CurrentUser;
import com.heimdallr.monitor.common.security.TokenPrincipalService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("db")
public class DbTokenPrincipalService extends TokenPrincipalService {
    private final JdbcClient jdbc;

    public DbTokenPrincipalService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CurrentUser> authenticate(String rawAuthorization) {
        String token = normalizeToken(rawAuthorization);
        String username = switch (token) {
            case "admin-token", "platform-admin", "u-admin" -> "admin";
            case "sre-token", "sre", "u-sre" -> "sre001";
            case "ace-owner-token", "ace-owner", "u-ace-owner" -> "user001";
            default -> token;
        };
        return userIdByUsername(username).map(userId -> currentUser(userId, token));
    }

    private CurrentUser currentUser(UUID userId, String token) {
        UserInfo user = jdbc.sql("""
                SELECT u.id, u.username, u.display_name
                FROM user_account u
                WHERE u.id = :userId AND u.status = 'active' AND u.deleted_at IS NULL
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> new UserInfo(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        roles(userId),
                        businessLines(userId),
                        menus(userId)
                ))
                .single();
        return new CurrentUser(user, dataScope(userId, user), token);
    }

    private List<RoleInfo> roles(UUID userId) {
        return jdbc.sql("""
                SELECT r.id, r.code, r.name,
                       COALESCE(array_agg(DISTINCT p.code ORDER BY p.code) FILTER (WHERE p.code IS NOT NULL), ARRAY[]::text[]) AS permissions
                FROM user_role ur
                JOIN role r ON r.id = ur.role_id
                LEFT JOIN role_permission rp ON rp.role_id = r.id
                LEFT JOIN permission p ON p.id = rp.permission_id AND p.status = 'active' AND p.deleted_at IS NULL
                WHERE ur.user_id = :userId
                  AND ur.deleted_at IS NULL
                  AND (ur.expires_at IS NULL OR ur.expires_at > now())
                  AND r.status = 'active'
                  AND r.deleted_at IS NULL
                GROUP BY r.id, r.code, r.name
                ORDER BY r.code
                """)
                .param("userId", userId)
                .query(this::roleInfo)
                .list();
    }

    private RoleInfo roleInfo(ResultSet rs, int rowNum) throws SQLException {
        return new RoleInfo(
                rs.getString("id"),
                rs.getString("code").toUpperCase(),
                rs.getString("name"),
                Set.of((String[]) rs.getArray("permissions").getArray())
        );
    }

    private DataScope dataScope(UUID userId, UserInfo user) {
        boolean platformAdmin = user.roles().stream().anyMatch(role -> "PLATFORM_ADMIN".equals(role.code()));
        return new DataScope(
                platformAdmin,
                platformAdmin ? Set.of() : applicationIds(userId),
                platformAdmin ? Set.of() : businessLines(userId),
                platformAdmin ? allEnvironments() : environments(userId)
        );
    }

    private Set<String> applicationIds(UUID userId) {
        return Set.copyOf(jdbc.sql("""
                SELECT DISTINCT application_id::text AS value
                FROM application_authorization
                WHERE user_id = :userId AND status = 'active' AND deleted_at IS NULL
                  AND (valid_until IS NULL OR valid_until > now())
                UNION
                SELECT DISTINCT application_id::text AS value
                FROM user_role
                WHERE user_id = :userId AND application_id IS NOT NULL AND deleted_at IS NULL
                  AND (expires_at IS NULL OR expires_at > now())
                """)
                .param("userId", userId)
                .query(String.class)
                .list());
    }

    private Set<String> businessLines(UUID userId) {
        return Set.copyOf(jdbc.sql("""
                SELECT DISTINCT bl.code AS value
                FROM business_line_authorization ba
                JOIN business_line bl ON bl.id = ba.business_line_id
                WHERE ba.user_id = :userId AND ba.status = 'active' AND ba.deleted_at IS NULL
                  AND (ba.valid_until IS NULL OR ba.valid_until > now())
                UNION
                SELECT DISTINCT bl.code AS value
                FROM user_role ur
                JOIN business_line bl ON bl.id = ur.business_line_id
                WHERE ur.user_id = :userId AND ur.business_line_id IS NOT NULL AND ur.deleted_at IS NULL
                  AND (ur.expires_at IS NULL OR ur.expires_at > now())
                """)
                .param("userId", userId)
                .query(String.class)
                .list());
    }

    private Set<String> environments(UUID userId) {
        List<String> values = jdbc.sql("""
                SELECT DISTINCT env AS value
                FROM application_authorization
                WHERE user_id = :userId AND status = 'active' AND deleted_at IS NULL AND env IS NOT NULL
                UNION
                SELECT DISTINCT env AS value
                FROM user_role
                WHERE user_id = :userId AND deleted_at IS NULL AND env IS NOT NULL
                """)
                .param("userId", userId)
                .query(String.class)
                .list();
        return values.isEmpty() ? Set.of("prod") : Set.copyOf(values);
    }

    private Set<String> allEnvironments() {
        List<String> values = jdbc.sql("""
                SELECT item_code
                FROM dictionary_item
                WHERE dict_type = 'env' AND enabled = true AND deleted_at IS NULL
                ORDER BY sort_order
                """)
                .query(String.class)
                .list();
        return values.isEmpty() ? Set.of("prod", "pre", "test", "dev") : Set.copyOf(values);
    }

    private Set<String> menus(UUID userId) {
        Set<String> resources = Set.copyOf(jdbc.sql("""
                SELECT DISTINCT COALESCE(p.resource, split_part(p.code, ':', 1)) AS value
                FROM user_role ur
                JOIN role_permission rp ON rp.role_id = ur.role_id
                JOIN permission p ON p.id = rp.permission_id
                WHERE ur.user_id = :userId AND ur.deleted_at IS NULL
                """)
                .param("userId", userId)
                .query(String.class)
                .list());
        return resources.isEmpty() ? Set.of("applications") : resources;
    }

    private Optional<UUID> userIdByUsername(String username) {
        return jdbc.sql("SELECT id FROM user_account WHERE username = :username AND deleted_at IS NULL")
                .param("username", username)
                .query(UUID.class)
                .optional();
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
