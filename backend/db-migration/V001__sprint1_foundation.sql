CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE dictionary_item (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  dict_type varchar(64) NOT NULL,
  item_code varchar(64) NOT NULL,
  item_label varchar(128) NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  enabled boolean NOT NULL DEFAULT true,
  description text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_dictionary_item_type_code UNIQUE (dict_type, item_code)
);

CREATE TABLE business_line (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(64) NOT NULL,
  name varchar(128) NOT NULL,
  owner_user_id uuid,
  status varchar(32) NOT NULL DEFAULT 'active',
  description text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_business_line_code UNIQUE (code)
);

CREATE TABLE department (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  business_line_id uuid REFERENCES business_line(id),
  parent_id uuid REFERENCES department(id),
  code varchar(64) NOT NULL,
  name varchar(128) NOT NULL,
  owner_user_id uuid,
  status varchar(32) NOT NULL DEFAULT 'active',
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_department_business_code UNIQUE (business_line_id, code)
);

CREATE TABLE user_account (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  username varchar(64) NOT NULL,
  display_name varchar(128) NOT NULL,
  email varchar(255),
  mobile varchar(32),
  employee_no varchar(64),
  department_id uuid REFERENCES department(id),
  status varchar(32) NOT NULL DEFAULT 'active',
  auth_source varchar(32) NOT NULL DEFAULT 'local',
  password_hash varchar(255),
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_user_account_username UNIQUE (username),
  CONSTRAINT uk_user_account_email UNIQUE (email)
);

ALTER TABLE business_line
  ADD CONSTRAINT fk_business_line_owner FOREIGN KEY (owner_user_id) REFERENCES user_account(id);

ALTER TABLE department
  ADD CONSTRAINT fk_department_owner FOREIGN KEY (owner_user_id) REFERENCES user_account(id);

CREATE TABLE application (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  business_line_id uuid NOT NULL REFERENCES business_line(id),
  department_id uuid REFERENCES department(id),
  code varchar(64) NOT NULL,
  name varchar(128) NOT NULL,
  system_name varchar(128),
  architecture_type varchar(64),
  tech_stack text[] NOT NULL DEFAULT ARRAY[]::text[],
  env varchar(32) NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'active',
  tags text[] NOT NULL DEFAULT ARRAY[]::text[],
  repository_url text,
  health_check_path text,
  deployment_type varchar(64),
  access_status varchar(64) NOT NULL DEFAULT 'not_connected',
  business_health_status varchar(32) NOT NULL DEFAULT 'unknown',
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_application_business_code_env UNIQUE (business_line_id, code, env)
);

CREATE TABLE application_owner (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  application_id uuid NOT NULL REFERENCES application(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES user_account(id),
  owner_role varchar(32) NOT NULL,
  notify_priority integer NOT NULL DEFAULT 1,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_application_owner_role UNIQUE (application_id, user_id, owner_role)
);

CREATE TABLE server (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  business_line_id uuid NOT NULL REFERENCES business_line(id),
  hostname varchar(128) NOT NULL,
  ip inet NOT NULL,
  public_ip inet,
  env varchar(32) NOT NULL,
  region varchar(64),
  zone varchar(64),
  os_type varchar(128),
  server_type varchar(64),
  spec varchar(128),
  status varchar(32) NOT NULL DEFAULT 'active',
  owner_user_id uuid REFERENCES user_account(id),
  access_status varchar(64) NOT NULL DEFAULT 'not_connected',
  business_health_status varchar(32) NOT NULL DEFAULT 'unknown',
  agent_install_status varchar(64) NOT NULL DEFAULT 'not_installed',
  agent_status varchar(64) NOT NULL DEFAULT 'not_installed',
  agent_version varchar(64),
  last_heartbeat_at timestamptz,
  tags text[] NOT NULL DEFAULT ARRAY[]::text[],
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_server_ip_env UNIQUE (ip, env),
  CONSTRAINT uk_server_hostname_env UNIQUE (hostname, env)
);

CREATE TABLE application_instance (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  application_id uuid NOT NULL REFERENCES application(id) ON DELETE CASCADE,
  server_id uuid NOT NULL REFERENCES server(id),
  instance_name varchar(128) NOT NULL,
  ip inet NOT NULL,
  port integer NOT NULL,
  protocol varchar(16) NOT NULL DEFAULT 'http',
  version varchar(128),
  status varchar(32) NOT NULL DEFAULT 'running',
  access_status varchar(64) NOT NULL DEFAULT 'not_connected',
  started_at timestamptz,
  last_seen_at timestamptz,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_application_instance_endpoint UNIQUE (application_id, server_id, ip, port)
);

CREATE TABLE app_server_binding (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  application_id uuid NOT NULL REFERENCES application(id) ON DELETE CASCADE,
  server_id uuid NOT NULL REFERENCES server(id) ON DELETE CASCADE,
  env varchar(32) NOT NULL,
  binding_role varchar(64) NOT NULL DEFAULT 'app',
  is_primary boolean NOT NULL DEFAULT false,
  source varchar(64) NOT NULL DEFAULT 'manual_import',
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_app_server_binding UNIQUE (application_id, server_id, binding_role)
);

CREATE TABLE role (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(64) NOT NULL,
  name varchar(128) NOT NULL,
  role_type varchar(32) NOT NULL DEFAULT 'business',
  status varchar(32) NOT NULL DEFAULT 'active',
  description text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_role_code UNIQUE (code)
);

CREATE TABLE permission (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL,
  name varchar(128) NOT NULL,
  permission_type varchar(32) NOT NULL,
  parent_id uuid REFERENCES permission(id),
  resource varchar(128),
  action varchar(64),
  sort_order integer NOT NULL DEFAULT 0,
  status varchar(32) NOT NULL DEFAULT 'active',
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_permission_code UNIQUE (code)
);

CREATE TABLE user_role (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  role_id uuid NOT NULL REFERENCES role(id) ON DELETE CASCADE,
  scope_type varchar(32) NOT NULL DEFAULT 'global',
  business_line_id uuid REFERENCES business_line(id),
  application_id uuid REFERENCES application(id),
  env varchar(32),
  granted_by uuid REFERENCES user_account(id),
  granted_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE TABLE role_permission (
  role_id uuid NOT NULL REFERENCES role(id) ON DELETE CASCADE,
  permission_id uuid NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE application_authorization (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  application_id uuid NOT NULL REFERENCES application(id) ON DELETE CASCADE,
  business_line_id uuid NOT NULL REFERENCES business_line(id),
  env varchar(32) NOT NULL,
  access_level varchar(32) NOT NULL DEFAULT 'read',
  granted_by uuid REFERENCES user_account(id),
  grant_reason text,
  valid_from timestamptz NOT NULL DEFAULT now(),
  valid_until timestamptz,
  status varchar(32) NOT NULL DEFAULT 'active',
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_application_authorization UNIQUE (user_id, application_id, env)
);

CREATE TABLE business_line_authorization (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  business_line_id uuid NOT NULL REFERENCES business_line(id) ON DELETE CASCADE,
  access_level varchar(32) NOT NULL DEFAULT 'read',
  granted_by uuid REFERENCES user_account(id),
  grant_reason text,
  valid_from timestamptz NOT NULL DEFAULT now(),
  valid_until timestamptz,
  status varchar(32) NOT NULL DEFAULT 'active',
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_business_line_authorization UNIQUE (user_id, business_line_id, access_level)
);

CREATE TABLE sensitive_access_grant (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  granted_by uuid REFERENCES user_account(id),
  scope_type varchar(32) NOT NULL,
  resource_type varchar(64) NOT NULL,
  resource_id uuid,
  business_line_id uuid REFERENCES business_line(id),
  application_id uuid REFERENCES application(id),
  env varchar(32),
  sensitive_type varchar(64) NOT NULL,
  reason text NOT NULL,
  valid_from timestamptz NOT NULL DEFAULT now(),
  valid_until timestamptz NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'active',
  approved_at timestamptz,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE TABLE mask_policy (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(64) NOT NULL,
  name varchar(128) NOT NULL,
  policy_type varchar(32) NOT NULL DEFAULT 'log',
  status varchar(32) NOT NULL DEFAULT 'active',
  is_default boolean NOT NULL DEFAULT false,
  description text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_mask_policy_code UNIQUE (code)
);

CREATE TABLE mask_rule (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  mask_policy_id uuid NOT NULL REFERENCES mask_policy(id) ON DELETE CASCADE,
  field_pattern varchar(255) NOT NULL,
  data_type varchar(64) NOT NULL,
  mask_type varchar(64) NOT NULL,
  mask_expression text,
  priority integer NOT NULL DEFAULT 100,
  enabled boolean NOT NULL DEFAULT true,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE TABLE export_task (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_by uuid NOT NULL REFERENCES user_account(id),
  export_type varchar(64) NOT NULL,
  scope_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  mask_policy_id uuid REFERENCES mask_policy(id),
  status varchar(32) NOT NULL DEFAULT 'pending',
  file_ref text,
  requested_at timestamptz NOT NULL DEFAULT now(),
  finished_at timestamptz,
  expired_at timestamptz,
  failure_reason text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE TABLE audit_event (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  operator_user_id uuid REFERENCES user_account(id),
  action varchar(128) NOT NULL,
  resource_type varchar(64) NOT NULL,
  resource_id uuid,
  business_line_id uuid REFERENCES business_line(id),
  app_id uuid REFERENCES application(id),
  client_ip inet,
  result varchar(32) NOT NULL,
  operated_at timestamptz NOT NULL DEFAULT now(),
  detail jsonb NOT NULL DEFAULT '{}'::jsonb,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE TABLE external_event_outbox (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_type varchar(128) NOT NULL,
  aggregate_type varchar(64) NOT NULL,
  aggregate_id uuid NOT NULL,
  payload jsonb NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'pending',
  retry_count integer NOT NULL DEFAULT 0,
  next_retry_at timestamptz,
  published_at timestamptz,
  last_error text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE TABLE retention_policy (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  resource_type varchar(64) NOT NULL,
  retention_days integer NOT NULL,
  archive_days integer,
  enabled boolean NOT NULL DEFAULT true,
  description text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_retention_policy_resource UNIQUE (resource_type)
);

CREATE INDEX idx_application_business_env_status ON application (business_line_id, env, status);
CREATE INDEX idx_application_code_env ON application (code, env);
CREATE INDEX idx_server_business_env_status ON server (business_line_id, env, status);
CREATE INDEX idx_server_ip_env ON server (ip, env);
CREATE INDEX idx_application_authorization_user_app_env ON application_authorization (user_id, application_id, env);
CREATE INDEX idx_audit_event_operator_time ON audit_event (operator_user_id, operated_at DESC);
CREATE INDEX idx_audit_event_resource_time ON audit_event (resource_type, resource_id, operated_at DESC);
CREATE INDEX idx_export_task_created_by_time ON export_task (created_by, created_at DESC);
CREATE INDEX idx_export_task_status_time ON export_task (status, created_at DESC);
CREATE INDEX idx_sensitive_access_user_valid ON sensitive_access_grant (user_id, status, valid_until);
CREATE INDEX idx_external_event_outbox_status_retry ON external_event_outbox (status, next_retry_at);

CREATE TRIGGER trg_dictionary_item_updated_at BEFORE UPDATE ON dictionary_item FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_business_line_updated_at BEFORE UPDATE ON business_line FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_department_updated_at BEFORE UPDATE ON department FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_user_account_updated_at BEFORE UPDATE ON user_account FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_application_updated_at BEFORE UPDATE ON application FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_application_owner_updated_at BEFORE UPDATE ON application_owner FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_server_updated_at BEFORE UPDATE ON server FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_application_instance_updated_at BEFORE UPDATE ON application_instance FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_app_server_binding_updated_at BEFORE UPDATE ON app_server_binding FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_role_updated_at BEFORE UPDATE ON role FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_permission_updated_at BEFORE UPDATE ON permission FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_user_role_updated_at BEFORE UPDATE ON user_role FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_application_authorization_updated_at BEFORE UPDATE ON application_authorization FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_business_line_authorization_updated_at BEFORE UPDATE ON business_line_authorization FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_sensitive_access_grant_updated_at BEFORE UPDATE ON sensitive_access_grant FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_mask_policy_updated_at BEFORE UPDATE ON mask_policy FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_mask_rule_updated_at BEFORE UPDATE ON mask_rule FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_export_task_updated_at BEFORE UPDATE ON export_task FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_audit_event_updated_at BEFORE UPDATE ON audit_event FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_external_event_outbox_updated_at BEFORE UPDATE ON external_event_outbox FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_retention_policy_updated_at BEFORE UPDATE ON retention_policy FOR EACH ROW EXECUTE FUNCTION set_updated_at();
