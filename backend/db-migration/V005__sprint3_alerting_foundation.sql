CREATE TABLE IF NOT EXISTS on_call_group (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL,
  name varchar(128) NOT NULL,
  business_line_id uuid REFERENCES business_line(id),
  status varchar(32) NOT NULL DEFAULT 'active',
  description text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_on_call_group_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS on_call_group_member (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  group_id uuid NOT NULL REFERENCES on_call_group(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES user_account(id),
  member_role varchar(32) NOT NULL DEFAULT 'member',
  notify_priority integer NOT NULL DEFAULT 1,
  status varchar(32) NOT NULL DEFAULT 'active',
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_on_call_group_member UNIQUE (group_id, user_id, member_role)
);

CREATE TABLE IF NOT EXISTS alert_rule (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  rule_name varchar(128) NOT NULL,
  object_id varchar(128) NOT NULL,
  metric_code varchar(128) NOT NULL,
  operator varchar(16) NOT NULL,
  threshold numeric(18, 4) NOT NULL,
  window_seconds integer NOT NULL DEFAULT 300,
  duration_seconds integer NOT NULL DEFAULT 60,
  evaluation_interval_seconds integer NOT NULL DEFAULT 60,
  severity varchar(16) NOT NULL DEFAULT 'P2',
  enabled boolean NOT NULL DEFAULT false,
  status varchar(32) NOT NULL DEFAULT 'active',
  business_line_id uuid REFERENCES business_line(id),
  app_id uuid REFERENCES application(id),
  on_call_group_id uuid REFERENCES on_call_group(id),
  created_by uuid REFERENCES user_account(id),
  updated_by uuid REFERENCES user_account(id),
  last_evaluated_at timestamptz,
  last_error text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT ck_alert_rule_operator CHECK (operator IN ('>', '>=', '<', '<=', '=', '!=')),
  CONSTRAINT ck_alert_rule_timing CHECK (window_seconds > 0 AND duration_seconds >= 0 AND evaluation_interval_seconds > 0)
);

CREATE TABLE IF NOT EXISTS alert_event (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  rule_id uuid NOT NULL REFERENCES alert_rule(id) ON DELETE CASCADE,
  dedup_key varchar(255) NOT NULL,
  object_id varchar(128) NOT NULL,
  metric_code varchar(128) NOT NULL,
  severity varchar(16) NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'triggered',
  trigger_value numeric(18, 4) NOT NULL,
  threshold numeric(18, 4) NOT NULL,
  operator varchar(16) NOT NULL,
  assignee_user_id uuid REFERENCES user_account(id),
  close_reason text,
  triggered_at timestamptz NOT NULL DEFAULT now(),
  notified_at timestamptz,
  acknowledged_at timestamptz,
  processing_at timestamptz,
  recovered_at timestamptz,
  closed_at timestamptz,
  last_seen_at timestamptz NOT NULL DEFAULT now(),
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_alert_event_active_dedup
  ON alert_event (dedup_key)
  WHERE deleted_at IS NULL AND status NOT IN ('recovered', 'closed');

CREATE TABLE IF NOT EXISTS alert_event_history (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL REFERENCES alert_event(id) ON DELETE CASCADE,
  from_status varchar(32),
  to_status varchar(32) NOT NULL,
  action varchar(64) NOT NULL,
  operator_user_id uuid REFERENCES user_account(id),
  message text,
  detail jsonb NOT NULL DEFAULT '{}'::jsonb,
  operated_at timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE TABLE IF NOT EXISTS notification_record (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL REFERENCES alert_event(id) ON DELETE CASCADE,
  rule_id uuid NOT NULL REFERENCES alert_rule(id) ON DELETE CASCADE,
  channel_type varchar(32) NOT NULL DEFAULT 'email',
  receiver varchar(255) NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'pending',
  retry_count integer NOT NULL DEFAULT 0,
  max_retry_count integer NOT NULL DEFAULT 3,
  failure_reason text,
  next_retry_at timestamptz,
  sent_at timestamptz,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

INSERT INTO permission (code, name, permission_type, resource, action, sort_order)
VALUES
  ('alerts:read', 'Read Alerts', 'action', 'alerts', 'read', 210),
  ('alerts:write', 'Write Alerts', 'action', 'alerts', 'write', 220)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    resource = EXCLUDED.resource,
    action = EXCLUDED.action,
    updated_at = now();

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON
  (r.code = 'platform_admin' AND p.code IN ('alerts:read','alerts:write'))
  OR (r.code = 'sre' AND p.code IN ('alerts:read','alerts:write'))
  OR (r.code = 'app_owner' AND p.code IN ('alerts:read','alerts:write'))
  OR (r.code = 'viewer' AND p.code IN ('alerts:read'))
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO on_call_group (id, code, name, business_line_id, status, description)
VALUES
  ('80000000-0000-0000-0000-000000000001', 'logistics-oncall', 'Logistics On-call', '10000000-0000-0000-0000-000000000001', 'active', 'Sprint 3 seed on-call group for ACE/Kafka'),
  ('80000000-0000-0000-0000-000000000099', 'sre-oncall', 'SRE On-call', '10000000-0000-0000-0000-000000000099', 'active', 'Sprint 3 shared SRE fallback group')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO on_call_group_member (group_id, user_id, member_role, notify_priority)
VALUES
  ('80000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000101', 'primary', 1),
  ('80000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000102', 'backup', 2),
  ('80000000-0000-0000-0000-000000000099', '00000000-0000-0000-0000-000000000011', 'primary', 1)
ON CONFLICT (group_id, user_id, member_role) DO UPDATE
SET notify_priority = EXCLUDED.notify_priority,
    updated_at = now();

CREATE INDEX IF NOT EXISTS idx_alert_rule_enabled_interval ON alert_rule (enabled, evaluation_interval_seconds, updated_at);
CREATE INDEX IF NOT EXISTS idx_alert_rule_object_metric ON alert_rule (object_id, metric_code);
CREATE INDEX IF NOT EXISTS idx_alert_event_rule_status ON alert_event (rule_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_alert_event_status_time ON alert_event (status, triggered_at DESC);
CREATE INDEX IF NOT EXISTS idx_alert_history_event_time ON alert_event_history (event_id, operated_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_event_time ON notification_record (event_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_status_retry ON notification_record (status, next_retry_at);

CREATE TRIGGER trg_on_call_group_updated_at BEFORE UPDATE ON on_call_group FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_on_call_group_member_updated_at BEFORE UPDATE ON on_call_group_member FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_alert_rule_updated_at BEFORE UPDATE ON alert_rule FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_alert_event_updated_at BEFORE UPDATE ON alert_event FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_alert_event_history_updated_at BEFORE UPDATE ON alert_event_history FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_notification_record_updated_at BEFORE UPDATE ON notification_record FOR EACH ROW EXECUTE FUNCTION set_updated_at();
