INSERT INTO dictionary_item (dict_type, item_code, item_label, sort_order, description)
VALUES
  ('env', 'prod', 'Production', 10, 'Production environment'),
  ('env', 'pre', 'Pre-release', 20, 'Pre-release environment'),
  ('env', 'test', 'Test', 30, 'Test environment'),
  ('env', 'dev', 'Development', 40, 'Development environment'),
  ('record_status', 'active', 'Active', 10, 'Enabled record'),
  ('record_status', 'inactive', 'Inactive', 20, 'Disabled record'),
  ('business_health_status', 'normal', 'Normal', 10, 'Business object is healthy'),
  ('business_health_status', 'warning', 'Warning', 20, 'Business object needs attention'),
  ('business_health_status', 'abnormal', 'Abnormal', 30, 'Business object is abnormal'),
  ('business_health_status', 'critical', 'Critical', 40, 'Business object is critical'),
  ('business_health_status', 'unknown', 'Unknown', 50, 'Business health is unknown'),
  ('access_status', 'connected', 'Connected', 10, 'Object and data source are connected'),
  ('access_status', 'not_connected', 'Not connected', 20, 'No data source or binding configured'),
  ('access_status', 'partial_connected', 'Partially connected', 30, 'Some monitoring capabilities are connected'),
  ('access_status', 'verifying', 'Verifying', 40, 'Connection and mapping are being verified'),
  ('access_status', 'collector_error', 'Collector error', 50, 'Agent or exporter is abnormal'),
  ('access_status', 'source_unavailable', 'Source unavailable', 60, 'Data source is unavailable'),
  ('access_status', 'no_recent_data', 'No recent data', 70, 'No recent samples collected'),
  ('access_status', 'mapping_invalid', 'Mapping invalid', 80, 'Label or field mapping is invalid'),
  ('agent_install_status', 'installed', 'Installed', 10, 'Agent is installed'),
  ('agent_install_status', 'not_installed', 'Not installed', 20, 'Agent is not installed'),
  ('agent_install_status', 'verifying', 'Verifying', 30, 'Agent installation is being verified'),
  ('agent_install_status', 'error', 'Error', 40, 'Agent installation has errors'),
  ('agent_status', 'normal', 'Normal', 10, 'Agent heartbeat is normal'),
  ('agent_status', 'no_heartbeat', 'No heartbeat', 20, 'Agent heartbeat is missing'),
  ('agent_status', 'version_low', 'Version too low', 30, 'Agent version is below baseline'),
  ('agent_status', 'config_error', 'Config error', 40, 'Agent config is invalid'),
  ('agent_status', 'not_installed', 'Not installed', 50, 'Agent is not installed'),
  ('export_status', 'pending', 'Pending', 10, 'Export task is pending'),
  ('export_status', 'running', 'Running', 20, 'Export task is running'),
  ('export_status', 'success', 'Success', 30, 'Export task completed'),
  ('export_status', 'failed', 'Failed', 40, 'Export task failed'),
  ('export_status', 'expired', 'Expired', 50, 'Export file expired'),
  ('audit_result', 'success', 'Success', 10, 'Operation succeeded'),
  ('audit_result', 'failed', 'Failed', 20, 'Operation failed')
ON CONFLICT (dict_type, item_code) DO UPDATE
SET item_label = EXCLUDED.item_label,
    sort_order = EXCLUDED.sort_order,
    description = EXCLUDED.description,
    updated_at = now();

INSERT INTO user_account (id, username, display_name, email, employee_no, status, auth_source, extra)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'admin', 'Platform Admin', 'admin@example.com', 'admin001', 'active', 'local', '{"seed": true}'),
  ('00000000-0000-0000-0000-000000000011', 'sre001', 'SRE Owner', 'sre001@example.com', 'sre001', 'active', 'local', '{"seed": true}'),
  ('00000000-0000-0000-0000-000000000101', 'user001', 'ACE Primary Owner', 'user001@example.com', 'user001', 'active', 'local', '{"seed": true}'),
  ('00000000-0000-0000-0000-000000000102', 'user002', 'ACE Backup Owner', 'user002@example.com', 'user002', 'active', 'local', '{"seed": true}'),
  ('00000000-0000-0000-0000-000000000201', 'user201', 'iPro Primary Owner', 'user201@example.com', 'user201', 'active', 'local', '{"seed": true}'),
  ('00000000-0000-0000-0000-000000000301', 'user301', 'CMS Primary Owner', 'user301@example.com', 'user301', 'active', 'local', '{"seed": true}')
ON CONFLICT (username) DO UPDATE
SET display_name = EXCLUDED.display_name,
    email = EXCLUDED.email,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO business_line (id, code, name, owner_user_id, status, description)
VALUES
  ('10000000-0000-0000-0000-000000000001', 'logistics', 'Logistics', '00000000-0000-0000-0000-000000000101', 'active', 'Sprint 1 core logistics line'),
  ('10000000-0000-0000-0000-000000000002', 'commerce', 'Commerce', '00000000-0000-0000-0000-000000000201', 'active', 'Sprint 1 commerce line'),
  ('10000000-0000-0000-0000-000000000003', 'content', 'Content', '00000000-0000-0000-0000-000000000301', 'active', 'Sprint 1 content line'),
  ('10000000-0000-0000-0000-000000000099', 'shared', 'Shared Platform', '00000000-0000-0000-0000-000000000011', 'active', 'Shared SRE managed assets')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    owner_user_id = EXCLUDED.owner_user_id,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO department (id, business_line_id, code, name, owner_user_id, status)
VALUES
  ('11000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'platform-rd', 'Platform R&D', '00000000-0000-0000-0000-000000000101', 'active'),
  ('11000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'commerce-rd', 'Commerce R&D', '00000000-0000-0000-0000-000000000201', 'active'),
  ('11000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'content-rd', 'Content R&D', '00000000-0000-0000-0000-000000000301', 'active'),
  ('11000000-0000-0000-0000-000000000099', '10000000-0000-0000-0000-000000000099', 'sre', 'SRE', '00000000-0000-0000-0000-000000000011', 'active')
ON CONFLICT (business_line_id, code) DO UPDATE
SET name = EXCLUDED.name,
    owner_user_id = EXCLUDED.owner_user_id,
    status = EXCLUDED.status,
    updated_at = now();

UPDATE user_account SET department_id = '11000000-0000-0000-0000-000000000099' WHERE username IN ('admin', 'sre001');
UPDATE user_account SET department_id = '11000000-0000-0000-0000-000000000001' WHERE username IN ('user001', 'user002');
UPDATE user_account SET department_id = '11000000-0000-0000-0000-000000000002' WHERE username = 'user201';
UPDATE user_account SET department_id = '11000000-0000-0000-0000-000000000003' WHERE username = 'user301';

INSERT INTO application (id, business_line_id, department_id, code, name, system_name, architecture_type, tech_stack, env, status, tags, repository_url, health_check_path, deployment_type, access_status, business_health_status, extra)
VALUES
  ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', 'ACE', 'ACE', 'Logistics Fulfillment', 'backend_service', ARRAY['Java','Spring Boot'], 'prod', 'active', ARRAY['core','java'], 'https://git.example.com/ace', '/actuator/health', 'VM', 'partial_connected', 'unknown', '{"oncall_group":"logistics-oncall"}'),
  ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', '11000000-0000-0000-0000-000000000002', 'IPRO', 'iPro', 'Commerce Product', 'backend_service', ARRAY['Java','Spring Boot'], 'prod', 'active', ARRAY['core','java'], 'https://git.example.com/ipro', '/actuator/health', 'VM', 'verifying', 'unknown', '{"oncall_group":"commerce-oncall"}'),
  ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', '11000000-0000-0000-0000-000000000003', 'CMS', 'CMS', 'Content Management', 'backend_service', ARRAY['Java','Spring Boot'], 'prod', 'active', ARRAY['core','content'], 'https://git.example.com/cms', '/actuator/health', 'VM', 'connected', 'normal', '{"oncall_group":"content-oncall"}')
ON CONFLICT (business_line_id, code, env) DO UPDATE
SET name = EXCLUDED.name,
    access_status = EXCLUDED.access_status,
    business_health_status = EXCLUDED.business_health_status,
    updated_at = now();

INSERT INTO application_owner (application_id, user_id, owner_role, notify_priority)
VALUES
  ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000101', 'primary', 1),
  ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000102', 'backup', 2),
  ('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000201', 'primary', 1),
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000301', 'primary', 1)
ON CONFLICT (application_id, user_id, owner_role) DO UPDATE
SET notify_priority = EXCLUDED.notify_priority,
    updated_at = now();

INSERT INTO server (id, business_line_id, hostname, ip, env, region, zone, os_type, server_type, spec, status, owner_user_id, access_status, business_health_status, agent_install_status, agent_status, agent_version, last_heartbeat_at, tags)
VALUES
  ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'app-prod-01', '10.0.1.10', 'prod', 'shanghai', 'shanghai-a', 'Linux 7.9', 'vm', '8C16G', 'active', '00000000-0000-0000-0000-000000000011', 'connected', 'normal', 'installed', 'normal', '1.0.0', now() - interval '5 minutes', ARRAY['core','java']),
  ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'app-prod-02', '10.0.1.11', 'prod', 'shanghai', 'shanghai-b', 'Linux 7.9', 'vm', '8C16G', 'active', '00000000-0000-0000-0000-000000000011', 'partial_connected', 'unknown', 'installed', 'version_low', '0.9.0', now() - interval '30 minutes', ARRAY['core','java']),
  ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'ipro-prod-01', '10.0.1.20', 'prod', 'shanghai', 'shanghai-a', 'Linux 7.9', 'vm', '8C16G', 'active', '00000000-0000-0000-0000-000000000011', 'verifying', 'unknown', 'installed', 'normal', '1.0.0', now() - interval '8 minutes', ARRAY['core','java']),
  ('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000003', 'cms-prod-01', '10.0.1.30', 'prod', 'shanghai', 'shanghai-a', 'Linux 7.9', 'vm', '4C8G', 'active', '00000000-0000-0000-0000-000000000011', 'connected', 'normal', 'installed', 'normal', '1.0.0', now() - interval '3 minutes', ARRAY['content','java'])
ON CONFLICT (ip, env) DO UPDATE
SET hostname = EXCLUDED.hostname,
    access_status = EXCLUDED.access_status,
    agent_status = EXCLUDED.agent_status,
    updated_at = now();

INSERT INTO app_server_binding (application_id, server_id, env, binding_role, is_primary, source)
VALUES
  ('20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'prod', 'app', true, 'seed'),
  ('20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', 'prod', 'app', false, 'seed'),
  ('20000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000003', 'prod', 'app', true, 'seed'),
  ('20000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000004', 'prod', 'app', true, 'seed')
ON CONFLICT (application_id, server_id, binding_role) DO UPDATE
SET is_primary = EXCLUDED.is_primary,
    updated_at = now();

INSERT INTO application_instance (application_id, server_id, instance_name, ip, port, protocol, version, status, access_status, started_at, last_seen_at)
VALUES
  ('20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'ACE-prod-10.0.1.10-8080', '10.0.1.10', 8080, 'http', 'v1.2.3', 'running', 'connected', now() - interval '2 days', now() - interval '5 minutes'),
  ('20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', 'ACE-prod-10.0.1.11-8080', '10.0.1.11', 8080, 'http', 'v1.2.3', 'running', 'partial_connected', now() - interval '2 days', now() - interval '30 minutes'),
  ('20000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000003', 'IPRO-prod-10.0.1.20-8080', '10.0.1.20', 8080, 'http', 'v2.0.1', 'running', 'verifying', now() - interval '1 day', now() - interval '8 minutes'),
  ('20000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000004', 'CMS-prod-10.0.1.30-8080', '10.0.1.30', 8080, 'http', 'v3.4.0', 'running', 'connected', now() - interval '3 days', now() - interval '3 minutes')
ON CONFLICT (application_id, server_id, ip, port) DO UPDATE
SET version = EXCLUDED.version,
    access_status = EXCLUDED.access_status,
    updated_at = now();

INSERT INTO role (id, code, name, role_type, status, description)
VALUES
  ('40000000-0000-0000-0000-000000000001', 'platform_admin', 'Platform Admin', 'system', 'active', 'Full platform administration'),
  ('40000000-0000-0000-0000-000000000002', 'sre', 'SRE', 'system', 'active', 'SRE operations across shared assets'),
  ('40000000-0000-0000-0000-000000000003', 'app_owner', 'Application Owner', 'business', 'active', 'Manage authorized applications'),
  ('40000000-0000-0000-0000-000000000004', 'viewer', 'Viewer', 'business', 'active', 'Read-only application visibility')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_at = now();

INSERT INTO permission (id, code, name, permission_type, resource, action, sort_order)
VALUES
  ('41000000-0000-0000-0000-000000000001', 'asset:read', 'Read Assets', 'action', 'asset', 'read', 10),
  ('41000000-0000-0000-0000-000000000002', 'asset:write', 'Write Assets', 'action', 'asset', 'write', 20),
  ('41000000-0000-0000-0000-000000000003', 'permission:read', 'Read Permissions', 'action', 'permission', 'read', 30),
  ('41000000-0000-0000-0000-000000000004', 'permission:write', 'Write Permissions', 'action', 'permission', 'write', 40),
  ('41000000-0000-0000-0000-000000000005', 'audit:read', 'Read Audit', 'action', 'audit', 'read', 50),
  ('41000000-0000-0000-0000-000000000006', 'export:create', 'Create Export', 'action', 'export', 'create', 60),
  ('41000000-0000-0000-0000-000000000007', 'sensitive:grant', 'Grant Sensitive Access', 'action', 'sensitive', 'grant', 70),
  ('41000000-0000-0000-0000-000000000008', 'sensitive:read_plain', 'Read Sensitive Plaintext', 'action', 'sensitive', 'read_plain', 80)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    resource = EXCLUDED.resource,
    action = EXCLUDED.action,
    updated_at = now();

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON
  (r.code = 'platform_admin')
  OR (r.code = 'sre' AND p.code IN ('asset:read','asset:write','audit:read','export:create','sensitive:grant'))
  OR (r.code = 'app_owner' AND p.code IN ('asset:read','export:create'))
  OR (r.code = 'viewer' AND p.code IN ('asset:read'))
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO user_role (user_id, role_id, scope_type, business_line_id, application_id, env, granted_by)
VALUES
  ('00000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'global', NULL, NULL, NULL, '00000000-0000-0000-0000-000000000001'),
  ('00000000-0000-0000-0000-000000000011', '40000000-0000-0000-0000-000000000002', 'business_line', '10000000-0000-0000-0000-000000000099', NULL, 'prod', '00000000-0000-0000-0000-000000000001'),
  ('00000000-0000-0000-0000-000000000101', '40000000-0000-0000-0000-000000000003', 'application', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'prod', '00000000-0000-0000-0000-000000000001'),
  ('00000000-0000-0000-0000-000000000201', '40000000-0000-0000-0000-000000000003', 'application', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'prod', '00000000-0000-0000-0000-000000000001'),
  ('00000000-0000-0000-0000-000000000301', '40000000-0000-0000-0000-000000000003', 'application', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 'prod', '00000000-0000-0000-0000-000000000001');

INSERT INTO application_authorization (user_id, application_id, business_line_id, env, access_level, granted_by, grant_reason)
VALUES
  ('00000000-0000-0000-0000-000000000101', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'prod', 'owner', '00000000-0000-0000-0000-000000000001', 'Seed ACE primary owner'),
  ('00000000-0000-0000-0000-000000000102', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'prod', 'read', '00000000-0000-0000-0000-000000000001', 'Seed ACE backup owner'),
  ('00000000-0000-0000-0000-000000000201', '20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'prod', 'owner', '00000000-0000-0000-0000-000000000001', 'Seed iPro owner'),
  ('00000000-0000-0000-0000-000000000301', '20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'prod', 'owner', '00000000-0000-0000-0000-000000000001', 'Seed CMS owner')
ON CONFLICT (user_id, application_id, env) DO UPDATE
SET access_level = EXCLUDED.access_level,
    grant_reason = EXCLUDED.grant_reason,
    updated_at = now();

INSERT INTO business_line_authorization (user_id, business_line_id, access_level, granted_by, grant_reason)
VALUES
  ('00000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000099', 'admin', '00000000-0000-0000-0000-000000000001', 'SRE manages shared platform assets'),
  ('00000000-0000-0000-0000-000000000101', '10000000-0000-0000-0000-000000000001', 'read', '00000000-0000-0000-0000-000000000001', 'ACE owner business-line visibility')
ON CONFLICT (user_id, business_line_id, access_level) DO UPDATE
SET grant_reason = EXCLUDED.grant_reason,
    updated_at = now();

INSERT INTO mask_policy (id, code, name, policy_type, status, is_default, description)
VALUES
  ('50000000-0000-0000-0000-000000000001', 'default-log-mask', 'Default Log Masking', 'log', 'active', true, 'Default Sprint 1 masking for logs, SQL, tokens, mobile and email'),
  ('50000000-0000-0000-0000-000000000002', 'default-export-mask', 'Default Export Masking', 'export', 'active', true, 'Default Sprint 1 masking for exported files')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    is_default = EXCLUDED.is_default,
    updated_at = now();

INSERT INTO mask_rule (mask_policy_id, field_pattern, data_type, mask_type, mask_expression, priority)
VALUES
  ('50000000-0000-0000-0000-000000000001', '(?i)(authorization|token|secret|cookie)', 'credential', 'fixed', '******', 10),
  ('50000000-0000-0000-0000-000000000001', '(?i)(sql|statement|query)', 'sql_text', 'partial', 'keep_keywords', 20),
  ('50000000-0000-0000-0000-000000000001', '(?i)(mobile|phone)', 'mobile', 'regex', 'keep_first3_last4', 30),
  ('50000000-0000-0000-0000-000000000001', '(?i)(email)', 'email', 'regex', 'email_user_mask', 40),
  ('50000000-0000-0000-0000-000000000002', '(?i)(connection|string|url|dsn)', 'connection_string', 'fixed', '******', 10);

INSERT INTO sensitive_access_grant (user_id, granted_by, scope_type, resource_type, resource_id, business_line_id, application_id, env, sensitive_type, reason, valid_until, approved_at)
VALUES
  ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001', 'application', 'application', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'prod', 'sql_text', 'Sprint 1 troubleshooting sample grant', now() + interval '4 hours', now());

INSERT INTO export_task (id, created_by, export_type, scope_json, mask_policy_id, status, file_ref, finished_at, expired_at)
VALUES
  ('60000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000101', 'application_inventory', '{"env":"prod","application_codes":["ACE"]}', '50000000-0000-0000-0000-000000000002', 'success', 'local://exports/seed/ace-application-inventory.csv', now() - interval '1 hour', now() + interval '7 days')
ON CONFLICT (id) DO UPDATE
SET status = EXCLUDED.status,
    file_ref = EXCLUDED.file_ref,
    updated_at = now();

INSERT INTO audit_event (operator_user_id, action, resource_type, resource_id, business_line_id, app_id, client_ip, result, detail)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'seed.init', 'database', NULL, NULL, NULL, '127.0.0.1', 'success', '{"batch":"sprint1_foundation"}'),
  ('00000000-0000-0000-0000-000000000101', 'sensitive.grant.use', 'application', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '10.0.1.50', 'success', '{"reason":"Sprint 1 troubleshooting sample grant","sensitive_type":"sql_text"}'),
  ('00000000-0000-0000-0000-000000000101', 'export.create', 'export_task', '60000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '10.0.1.50', 'success', '{"export_type":"application_inventory","masked":true}');

INSERT INTO external_event_outbox (event_type, aggregate_type, aggregate_id, payload, status, next_retry_at)
VALUES
  ('audit.created', 'audit_event', '60000000-0000-0000-0000-000000000001', '{"action":"export.create","seed":true}', 'pending', now() + interval '5 minutes');

INSERT INTO retention_policy (resource_type, retention_days, archive_days, enabled, description)
VALUES
  ('audit_event', 730, NULL, true, 'High-risk audit events retained for 24 months'),
  ('export_task', 365, NULL, true, 'Export tasks retained for 12 months'),
  ('export_file', 30, NULL, true, 'Export files retained for up to 30 days')
ON CONFLICT (resource_type) DO UPDATE
SET retention_days = EXCLUDED.retention_days,
    enabled = EXCLUDED.enabled,
    updated_at = now();
