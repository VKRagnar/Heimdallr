package com.heimdallr.monitor.api.repository;

import com.heimdallr.monitor.api.fixture.InMemoryMonitorData;
import com.heimdallr.monitor.common.domain.api.ErrorCode;
import com.heimdallr.monitor.common.domain.exception.ForbiddenException;
import com.heimdallr.monitor.common.domain.exception.NotFoundException;
import com.heimdallr.monitor.common.domain.model.ApplicationAsset;
import com.heimdallr.monitor.common.domain.model.AuditEvent;
import com.heimdallr.monitor.common.domain.model.RoleInfo;
import com.heimdallr.monitor.common.domain.model.ServerAsset;
import com.heimdallr.monitor.common.domain.model.UserInfo;
import com.heimdallr.monitor.common.security.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("db")
public class JdbcMonitorData extends InMemoryMonitorData {
    private static final Set<String> ALL_PERMISSIONS = Set.of(
            "applications:read",
            "applications:write",
            "servers:read",
            "servers:write",
            "audit:read",
            "access:read",
            "access:write",
            "data-sources:read",
            "data-sources:write",
            "agents:read",
            "metrics:read",
            "logs:read"
    );

    private final JdbcClient jdbc;

    public JdbcMonitorData(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ApplicationAsset> visibleApplications(CurrentUser currentUser) {
        String sql = """
                SELECT a.id, a.code, a.name, bl.code AS business_line, a.env, a.access_status,
                       COALESCE(array_agg(ao.user_id::text ORDER BY ao.notify_priority) FILTER (WHERE ao.user_id IS NOT NULL), ARRAY[]::text[]) AS owner_user_ids
                FROM application a
                JOIN business_line bl ON bl.id = a.business_line_id
                LEFT JOIN application_owner ao ON ao.application_id = a.id AND ao.deleted_at IS NULL
                WHERE a.deleted_at IS NULL
                  AND (:platformAdmin OR a.env = ANY(:envs) OR EXISTS (
                        SELECT 1 FROM application_authorization aa
                        WHERE aa.application_id = a.id AND aa.user_id = :userId AND aa.status = 'active' AND aa.deleted_at IS NULL
                  ) OR EXISTS (
                        SELECT 1 FROM business_line_authorization ba
                        WHERE ba.business_line_id = a.business_line_id AND ba.user_id = :userId AND ba.status = 'active' AND ba.deleted_at IS NULL
                  ))
                GROUP BY a.id, a.code, a.name, bl.code, a.env, a.access_status
                ORDER BY a.code
                """;
        return jdbc.sql(sql)
                .param("platformAdmin", currentUser.dataScope().platformAdmin())
                .param("envs", currentUser.dataScope().environments().toArray(String[]::new))
                .param("userId", resolveUserId(currentUser).orElse(null))
                .query(this::applicationAsset)
                .list();
    }

    @Override
    public ApplicationAsset requireVisibleApplication(String id, CurrentUser currentUser) {
        return visibleApplications(currentUser).stream()
                .filter(application -> application.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Application not found"));
    }

    @Override
    @Transactional
    public ApplicationAsset saveApplication(ApplicationAsset application, CurrentUser currentUser) {
        requirePermission(currentUser, "applications:write");
        if (!currentUser.dataScope().canAccessEnvironment(application.environment())) {
            throw new ForbiddenException(ErrorCode.ENV_FORBIDDEN, "Environment is outside current user scope");
        }
        UUID businessLineId = ensureBusinessLine(application.businessLine());
        UUID applicationId = application.id() == null || application.id().isBlank()
                ? UUID.randomUUID()
                : stableUuid(application.id());
        String status = normalizeStatus(application.accessStatus(), "connected");

        jdbc.sql("""
                INSERT INTO application (id, business_line_id, code, name, env, access_status)
                VALUES (:id, :businessLineId, :code, :name, :env, :accessStatus)
                ON CONFLICT (id) DO UPDATE
                SET business_line_id = EXCLUDED.business_line_id,
                    code = EXCLUDED.code,
                    name = EXCLUDED.name,
                    env = EXCLUDED.env,
                    access_status = EXCLUDED.access_status,
                    updated_at = now()
                """)
                .param("id", applicationId)
                .param("businessLineId", businessLineId)
                .param("code", application.code())
                .param("name", application.name())
                .param("env", application.environment())
                .param("accessStatus", status)
                .update();

        jdbc.sql("DELETE FROM application_owner WHERE application_id = :applicationId")
                .param("applicationId", applicationId)
                .update();
        for (String ownerUserId : Optional.ofNullable(application.ownerUserIds()).orElse(List.of())) {
            parseUuid(ownerUserId).ifPresent(userId -> jdbc.sql("""
                    INSERT INTO application_owner (application_id, user_id, owner_role, notify_priority)
                    VALUES (:applicationId, :userId, 'primary', 1)
                    ON CONFLICT (application_id, user_id, owner_role) DO NOTHING
                    """)
                    .param("applicationId", applicationId)
                    .param("userId", userId)
                    .update());
        }
        appendAudit(currentUser, "APPLICATION_UPSERT", "APPLICATION", applicationId, "success");
        return requireVisibleApplication(applicationId.toString(), currentUser);
    }

    @Override
    @Transactional
    public List<ApplicationAsset> importApplications(List<ApplicationAsset> imports, CurrentUser currentUser) {
        requirePermission(currentUser, "applications:write");
        return imports.stream().map(application -> saveApplication(application, currentUser)).toList();
    }

    @Override
    public List<ServerAsset> visibleServers(CurrentUser currentUser) {
        String sql = """
                SELECT s.id, s.hostname, s.ip::text AS ip, s.env, s.access_status,
                       COALESCE(array_agg(asb.application_id::text ORDER BY asb.application_id) FILTER (WHERE asb.application_id IS NOT NULL), ARRAY[]::text[]) AS application_ids
                FROM server s
                LEFT JOIN app_server_binding asb ON asb.server_id = s.id AND asb.deleted_at IS NULL
                WHERE s.deleted_at IS NULL
                  AND (:platformAdmin OR s.env = ANY(:envs) OR EXISTS (
                        SELECT 1 FROM app_server_binding binding
                        JOIN application_authorization aa ON aa.application_id = binding.application_id
                        WHERE binding.server_id = s.id AND aa.user_id = :userId AND aa.status = 'active' AND aa.deleted_at IS NULL
                  ))
                GROUP BY s.id, s.hostname, s.ip, s.env, s.access_status
                ORDER BY s.hostname
                """;
        return jdbc.sql(sql)
                .param("platformAdmin", currentUser.dataScope().platformAdmin())
                .param("envs", currentUser.dataScope().environments().toArray(String[]::new))
                .param("userId", resolveUserId(currentUser).orElse(null))
                .query(this::serverAsset)
                .list();
    }

    @Override
    @Transactional
    public ServerAsset saveServer(ServerAsset server, CurrentUser currentUser) {
        requirePermission(currentUser, "servers:write");
        if (!currentUser.dataScope().canAccessEnvironment(server.environment())) {
            throw new ForbiddenException(ErrorCode.ENV_FORBIDDEN, "Environment is outside current user scope");
        }
        Set<UUID> applicationIds = Optional.ofNullable(server.applicationIds()).orElse(Set.of()).stream()
                .map(this::requireApplicationId)
                .collect(Collectors.toSet());
        UUID businessLineId = applicationIds.stream()
                .findFirst()
                .flatMap(this::businessLineIdForApplication)
                .orElseGet(() -> ensureBusinessLine("shared"));
        UUID serverId = server.id() == null || server.id().isBlank()
                ? UUID.randomUUID()
                : stableUuid(server.id());

        jdbc.sql("""
                INSERT INTO server (id, business_line_id, hostname, ip, env, access_status)
                VALUES (:id, :businessLineId, :hostname, CAST(:ip AS inet), :env, :accessStatus)
                ON CONFLICT (id) DO UPDATE
                SET business_line_id = EXCLUDED.business_line_id,
                    hostname = EXCLUDED.hostname,
                    ip = EXCLUDED.ip,
                    env = EXCLUDED.env,
                    access_status = EXCLUDED.access_status,
                    updated_at = now()
                """)
                .param("id", serverId)
                .param("businessLineId", businessLineId)
                .param("hostname", server.hostname())
                .param("ip", server.ip())
                .param("env", server.environment())
                .param("accessStatus", normalizeStatus(server.accessStatus(), "connected"))
                .update();

        jdbc.sql("DELETE FROM app_server_binding WHERE server_id = :serverId")
                .param("serverId", serverId)
                .update();
        for (UUID applicationId : applicationIds) {
            jdbc.sql("""
                    INSERT INTO app_server_binding (application_id, server_id, env, binding_role, source)
                    VALUES (:applicationId, :serverId, :env, 'app', 'api')
                    ON CONFLICT (application_id, server_id, binding_role) DO UPDATE
                    SET env = EXCLUDED.env, updated_at = now()
                    """)
                    .param("applicationId", applicationId)
                    .param("serverId", serverId)
                    .param("env", server.environment())
                    .update();
        }
        appendAudit(currentUser, "SERVER_UPSERT", "SERVER", serverId, "success");
        return visibleServers(currentUser).stream()
                .filter(item -> item.id().equals(serverId.toString()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Server not found"));
    }

    @Override
    @Transactional
    public List<ServerAsset> importServers(List<ServerAsset> imports, CurrentUser currentUser) {
        requirePermission(currentUser, "servers:write");
        return imports.stream().map(server -> saveServer(server, currentUser)).toList();
    }

    @Override
    public List<AuditEvent> auditEvents(CurrentUser currentUser) {
        requirePermission(currentUser, "audit:read");
        return jdbc.sql("""
                SELECT ae.id, ae.operator_user_id::text AS actor_user_id, ae.action,
                       ae.resource_type, ae.resource_id::text AS resource_id, ae.result, ae.operated_at
                FROM audit_event ae
                WHERE ae.deleted_at IS NULL
                ORDER BY ae.operated_at, ae.id
                """)
                .query((rs, rowNum) -> new AuditEvent(
                        rs.getString("id"),
                        rs.getString("actor_user_id"),
                        rs.getString("action"),
                        rs.getString("resource_type"),
                        rs.getString("resource_id"),
                        rs.getString("result"),
                        rs.getObject("operated_at", OffsetDateTime.class)
                ))
                .list();
    }

    @Override
    public List<UserInfo> users(CurrentUser currentUser) {
        requirePermission(currentUser, "access:read");
        return jdbc.sql("""
                SELECT u.id, u.username, u.display_name,
                       COALESCE(array_agg(DISTINCT bl.code) FILTER (WHERE bl.code IS NOT NULL), ARRAY[]::text[]) AS business_lines
                FROM user_account u
                LEFT JOIN business_line_authorization ba ON ba.user_id = u.id AND ba.status = 'active' AND ba.deleted_at IS NULL
                LEFT JOIN business_line bl ON bl.id = ba.business_line_id
                WHERE u.deleted_at IS NULL
                GROUP BY u.id, u.username, u.display_name
                ORDER BY u.username
                """)
                .query((rs, rowNum) -> new UserInfo(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        userRoles(rs.getObject("id", UUID.class)),
                        Set.of((String[]) rs.getArray("business_lines").getArray()),
                        Set.of("assets", "access", "audit")
                ))
                .list();
    }

    @Override
    public List<RoleInfo> roles(CurrentUser currentUser) {
        requirePermission(currentUser, "access:read");
        return jdbc.sql("SELECT id, code, name FROM role WHERE deleted_at IS NULL ORDER BY code")
                .query((rs, rowNum) -> roleInfo(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name")))
                .list();
    }

    @Override
    @Transactional
    public UserInfo grantApplicationAccess(String userId, String applicationId, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UUID targetUserId = requireUserId(userId);
        UUID targetApplicationId = requireApplicationId(applicationId);
        UUID businessLineId = businessLineIdForApplication(targetApplicationId).orElseThrow(() -> new NotFoundException("Application not found"));
        jdbc.sql("""
                INSERT INTO application_authorization (user_id, application_id, business_line_id, env, access_level, granted_by, grant_reason)
                SELECT :userId, a.id, a.business_line_id, a.env, 'read', :grantedBy, 'Granted from API'
                FROM application a WHERE a.id = :applicationId
                ON CONFLICT (user_id, application_id, env) DO UPDATE
                SET status = 'active', granted_by = EXCLUDED.granted_by, updated_at = now()
                """)
                .param("userId", targetUserId)
                .param("applicationId", targetApplicationId)
                .param("grantedBy", resolveUserId(currentUser).orElse(null))
                .update();
        appendAudit(currentUser, "ACCESS_GRANT_APPLICATION", "USER", targetUserId, "success");
        return requireUser(targetUserId);
    }

    @Override
    @Transactional
    public UserInfo revokeApplicationAccess(String userId, String applicationId, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UUID targetUserId = requireUserId(userId);
        UUID targetApplicationId = requireApplicationId(applicationId);
        jdbc.sql("UPDATE application_authorization SET status = 'revoked', updated_at = now() WHERE user_id = :userId AND application_id = :applicationId")
                .param("userId", targetUserId)
                .param("applicationId", targetApplicationId)
                .update();
        appendAudit(currentUser, "ACCESS_REVOKE_APPLICATION", "USER", targetUserId, "success");
        return requireUser(targetUserId);
    }

    @Override
    @Transactional
    public UserInfo grantBusinessLineAccess(String userId, String businessLine, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UUID targetUserId = requireUserId(userId);
        UUID businessLineId = ensureBusinessLine(businessLine);
        jdbc.sql("""
                INSERT INTO business_line_authorization (user_id, business_line_id, access_level, granted_by, grant_reason)
                VALUES (:userId, :businessLineId, 'read', :grantedBy, 'Granted from API')
                ON CONFLICT (user_id, business_line_id, access_level) DO UPDATE
                SET status = 'active', granted_by = EXCLUDED.granted_by, updated_at = now()
                """)
                .param("userId", targetUserId)
                .param("businessLineId", businessLineId)
                .param("grantedBy", resolveUserId(currentUser).orElse(null))
                .update();
        appendAudit(currentUser, "ACCESS_GRANT_BUSINESS_LINE", "USER", targetUserId, "success");
        return requireUser(targetUserId);
    }

    @Override
    @Transactional
    public UserInfo revokeBusinessLineAccess(String userId, String businessLine, CurrentUser currentUser) {
        requirePermission(currentUser, "access:write");
        UUID targetUserId = requireUserId(userId);
        UUID businessLineId = ensureBusinessLine(businessLine);
        jdbc.sql("UPDATE business_line_authorization SET status = 'revoked', updated_at = now() WHERE user_id = :userId AND business_line_id = :businessLineId")
                .param("userId", targetUserId)
                .param("businessLineId", businessLineId)
                .update();
        appendAudit(currentUser, "ACCESS_REVOKE_BUSINESS_LINE", "USER", targetUserId, "success");
        return requireUser(targetUserId);
    }

    private ApplicationAsset applicationAsset(ResultSet rs, int rowNum) throws SQLException {
        return new ApplicationAsset(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("business_line"),
                rs.getString("env"),
                List.of((String[]) rs.getArray("owner_user_ids").getArray()),
                rs.getString("access_status")
        );
    }

    private ServerAsset serverAsset(ResultSet rs, int rowNum) throws SQLException {
        return new ServerAsset(
                rs.getString("id"),
                rs.getString("hostname"),
                rs.getString("ip"),
                rs.getString("env"),
                Set.of((String[]) rs.getArray("application_ids").getArray()),
                rs.getString("access_status")
        );
    }

    private UUID ensureBusinessLine(String code) {
        String normalized = code == null || code.isBlank() ? "shared" : code;
        return jdbc.sql("""
                INSERT INTO business_line (code, name)
                VALUES (:code, :name)
                ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
                RETURNING id
                """)
                .param("code", normalized)
                .param("name", normalized)
                .query(UUID.class)
                .single();
    }

    private Optional<UUID> businessLineIdForApplication(UUID applicationId) {
        return jdbc.sql("SELECT business_line_id FROM application WHERE id = :id AND deleted_at IS NULL")
                .param("id", applicationId)
                .query(UUID.class)
                .optional();
    }

    private UUID requireApplicationId(String applicationId) {
        UUID id = stableUuid(applicationId);
        boolean exists = jdbc.sql("SELECT EXISTS(SELECT 1 FROM application WHERE id = :id AND deleted_at IS NULL)")
                .param("id", id)
                .query(Boolean.class)
                .single();
        if (!exists) {
            throw new NotFoundException("Application not found");
        }
        return id;
    }

    private UUID requireUserId(String userId) {
        return parseUuid(userId)
                .filter(this::userExists)
                .or(() -> userIdByAlias(userId))
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private boolean userExists(UUID userId) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM user_account WHERE id = :id AND deleted_at IS NULL)")
                .param("id", userId)
                .query(Boolean.class)
                .single();
    }

    private UserInfo requireUser(UUID userId) {
        return jdbc.sql("""
                SELECT u.id, u.username, u.display_name,
                       COALESCE(array_agg(DISTINCT bl.code) FILTER (WHERE bl.code IS NOT NULL), ARRAY[]::text[]) AS business_lines
                FROM user_account u
                LEFT JOIN business_line_authorization ba ON ba.user_id = u.id AND ba.status = 'active' AND ba.deleted_at IS NULL
                LEFT JOIN business_line bl ON bl.id = ba.business_line_id
                WHERE u.id = :userId AND u.deleted_at IS NULL
                GROUP BY u.id, u.username, u.display_name
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> new UserInfo(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        userRoles(rs.getObject("id", UUID.class)),
                        Set.of((String[]) rs.getArray("business_lines").getArray()),
                        Set.of("assets", "access", "audit")
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private List<RoleInfo> userRoles(UUID userId) {
        List<RoleInfo> roles = jdbc.sql("""
                SELECT DISTINCT r.id, r.code, r.name
                FROM user_role ur
                JOIN role r ON r.id = ur.role_id
                WHERE ur.user_id = :userId AND ur.deleted_at IS NULL AND r.deleted_at IS NULL
                ORDER BY r.code
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> roleInfo(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name")))
                .list();
        return roles.isEmpty() ? List.of(new RoleInfo("viewer", "VIEWER", "Viewer", Set.of("applications:read", "servers:read"))) : roles;
    }

    private RoleInfo roleInfo(UUID id, String code, String name) {
        String normalized = code == null ? "" : code.toLowerCase();
        Set<String> permissions = switch (normalized) {
            case "platform_admin" -> ALL_PERMISSIONS;
            case "sre" -> Set.of("applications:read", "servers:read", "audit:read", "access:read", "data-sources:read", "data-sources:write", "agents:read", "metrics:read", "logs:read");
            case "app_owner" -> Set.of("applications:read", "servers:read", "agents:read", "metrics:read", "logs:read");
            default -> Set.of("applications:read", "servers:read");
        };
        return new RoleInfo(id.toString(), code == null ? "VIEWER" : code.toUpperCase(), name, permissions);
    }

    private Optional<UUID> resolveUserId(CurrentUser currentUser) {
        return parseUuid(currentUser.user().id()).filter(this::userExists)
                .or(() -> userIdByAlias(currentUser.user().username()))
                .or(() -> userIdByAlias(currentUser.token()));
    }

    private Optional<UUID> userIdByAlias(String alias) {
        String username = switch (alias) {
            case "platform-admin", "admin-token" -> "admin";
            case "sre", "sre-token" -> "sre001";
            case "ace-owner", "ace-owner-token", "u-ace-owner" -> "user001";
            case "u-admin" -> "admin";
            case "u-sre" -> "sre001";
            default -> alias;
        };
        return jdbc.sql("SELECT id FROM user_account WHERE username = :username AND deleted_at IS NULL")
                .param("username", username)
                .query(UUID.class)
                .optional();
    }

    private void appendAudit(CurrentUser currentUser, String action, String targetType, UUID targetId, String result) {
        jdbc.sql("""
                INSERT INTO audit_event (operator_user_id, action, resource_type, resource_id, result, detail)
                VALUES (:operatorUserId, :action, :resourceType, :resourceId, :result, '{}'::jsonb)
                """)
                .param("operatorUserId", resolveUserId(currentUser).orElse(null))
                .param("action", action)
                .param("resourceType", targetType)
                .param("resourceId", targetId)
                .param("result", result)
                .update();
    }

    private static String normalizeStatus(String status, String fallback) {
        return status == null || status.isBlank() ? fallback : status.toLowerCase();
    }

    private static UUID stableUuid(String value) {
        return parseUuid(value).orElseGet(() -> UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
