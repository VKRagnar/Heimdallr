CREATE TABLE IF NOT EXISTS monitor_object (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL,
  name varchar(128) NOT NULL,
  object_type varchar(64) NOT NULL,
  env varchar(32) NOT NULL,
  business_line_id uuid REFERENCES business_line(id),
  health_status varchar(32) NOT NULL DEFAULT 'unknown',
  access_status varchar(64) NOT NULL DEFAULT 'not_connected',
  key_metrics jsonb NOT NULL DEFAULT '{}'::jsonb,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_monitor_object_code_env UNIQUE (code, env)
);

CREATE TABLE IF NOT EXISTS object_app_dependency (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  object_id uuid NOT NULL REFERENCES monitor_object(id) ON DELETE CASCADE,
  application_id uuid NOT NULL REFERENCES application(id) ON DELETE CASCADE,
  dependency_role varchar(64) NOT NULL DEFAULT 'runtime',
  status varchar(32) NOT NULL DEFAULT 'active',
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_object_app_dependency UNIQUE (object_id, application_id, dependency_role)
);

CREATE TABLE IF NOT EXISTS object_server_binding (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  object_id uuid NOT NULL REFERENCES monitor_object(id) ON DELETE CASCADE,
  server_id uuid NOT NULL REFERENCES server(id) ON DELETE CASCADE,
  binding_role varchar(64) NOT NULL DEFAULT 'runtime',
  status varchar(32) NOT NULL DEFAULT 'active',
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_object_server_binding UNIQUE (object_id, server_id, binding_role)
);

CREATE TABLE IF NOT EXISTS data_source (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL,
  name varchar(128) NOT NULL,
  source_type varchar(64) NOT NULL,
  env varchar(32) NOT NULL,
  base_url text NOT NULL,
  health_check_path text,
  auth_type varchar(32) NOT NULL DEFAULT 'none',
  secret_ref text,
  timeout_seconds integer NOT NULL DEFAULT 5,
  retry_count integer NOT NULL DEFAULT 1,
  rate_limit_qps integer NOT NULL DEFAULT 20,
  status varchar(32) NOT NULL DEFAULT 'enabled',
  last_check_at timestamptz,
  last_success_at timestamptz,
  last_error_code varchar(128),
  last_error_message text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_data_source_code_env UNIQUE (code, env)
);

CREATE TABLE IF NOT EXISTS data_source_binding (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL,
  object_id uuid NOT NULL REFERENCES monitor_object(id) ON DELETE CASCADE,
  source_id uuid NOT NULL REFERENCES data_source(id) ON DELETE CASCADE,
  binding_type varchar(32) NOT NULL,
  external_labels jsonb NOT NULL DEFAULT '{}'::jsonb,
  mapping_config jsonb NOT NULL DEFAULT '{}'::jsonb,
  last_seen_at timestamptz,
  access_status varchar(64) NOT NULL DEFAULT 'not_connected',
  failure_reason text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_data_source_binding_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS agent_instance (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL,
  server_id uuid NOT NULL REFERENCES server(id) ON DELETE CASCADE,
  version varchar(64),
  status varchar(64) NOT NULL DEFAULT 'not_installed',
  last_heartbeat_at timestamptz,
  config_version varchar(128),
  failure_reason text,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_agent_instance_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS metric_definition (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL,
  name varchar(128) NOT NULL,
  object_type varchar(64) NOT NULL,
  category varchar(64) NOT NULL,
  unit varchar(32) NOT NULL,
  source_type varchar(64) NOT NULL,
  default_query_template text NOT NULL,
  labels text[] NOT NULL DEFAULT ARRAY[]::text[],
  status varchar(32) NOT NULL DEFAULT 'active',
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_metric_definition_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS metric_series_mapping (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL,
  object_type varchar(64) NOT NULL,
  metric_code varchar(128) NOT NULL,
  source_type varchar(64) NOT NULL,
  external_metric varchar(128) NOT NULL,
  query_template text NOT NULL,
  unit varchar(32) NOT NULL,
  default_labels jsonb NOT NULL DEFAULT '{}'::jsonb,
  status varchar(32) NOT NULL DEFAULT 'active',
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_metric_series_mapping_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS log_entry_sample (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL,
  occurred_at timestamptz NOT NULL,
  application_id uuid NOT NULL REFERENCES application(id),
  object_id uuid REFERENCES monitor_object(id),
  env varchar(32) NOT NULL,
  level varchar(16) NOT NULL,
  message text NOT NULL,
  trace_id varchar(128),
  source_id uuid REFERENCES data_source(id),
  labels jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_log_entry_sample_code UNIQUE (code)
);

INSERT INTO permission (code, name, permission_type, resource, action, sort_order)
VALUES
  ('applications:read', 'Read Applications', 'action', 'applications', 'read', 100),
  ('applications:write', 'Write Applications', 'action', 'applications', 'write', 110),
  ('servers:read', 'Read Servers', 'action', 'servers', 'read', 120),
  ('servers:write', 'Write Servers', 'action', 'servers', 'write', 130),
  ('access:read', 'Read Access Management', 'action', 'access', 'read', 140),
  ('access:write', 'Write Access Management', 'action', 'access', 'write', 150),
  ('data-sources:read', 'Read Data Sources', 'action', 'data-sources', 'read', 160),
  ('data-sources:write', 'Write Data Sources', 'action', 'data-sources', 'write', 170),
  ('agents:read', 'Read Agents', 'action', 'agents', 'read', 180),
  ('metrics:read', 'Read Metrics', 'action', 'metrics', 'read', 190),
  ('logs:read', 'Read Logs', 'action', 'logs', 'read', 200)
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
  OR (r.code = 'sre' AND p.code IN ('applications:read','servers:read','audit:read','access:read','data-sources:read','data-sources:write','agents:read','metrics:read','logs:read','export:create','sensitive:grant'))
  OR (r.code = 'app_owner' AND p.code IN ('applications:read','servers:read','agents:read','metrics:read','logs:read','export:create'))
  OR (r.code = 'viewer' AND p.code IN ('applications:read','servers:read'))
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO monitor_object (id, code, name, object_type, env, business_line_id, health_status, access_status, key_metrics)
VALUES
  ('70000000-0000-0000-0000-000000000001', 'obj-kafka-orders', 'orders-kafka', 'KAFKA', 'prod', '10000000-0000-0000-0000-000000000001', 'WARN', 'CONNECTED', '{"mq_lag":"1280","broker_up":"3/3"}'),
  ('70000000-0000-0000-0000-000000000002', 'obj-pg-ace', 'ace-postgresql', 'POSTGRESQL', 'prod', '10000000-0000-0000-0000-000000000001', 'HEALTHY', 'CONNECTED', '{"db_conn_usage":"61%","slow_sql_count":"3"}'),
  ('70000000-0000-0000-0000-000000000003', 'obj-ipro-api', 'ipro-api', 'APPLICATION', 'prod', '10000000-0000-0000-0000-000000000002', 'WARN', 'SOURCE_UNAVAILABLE', '{"http_5xx_rate":"1.8%","p95_latency":"860ms"}')
ON CONFLICT (code, env) DO UPDATE
SET name = EXCLUDED.name,
    health_status = EXCLUDED.health_status,
    access_status = EXCLUDED.access_status,
    key_metrics = EXCLUDED.key_metrics,
    updated_at = now();

INSERT INTO object_app_dependency (object_id, application_id, dependency_role)
VALUES
  ('70000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'producer'),
  ('70000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'consumer'),
  ('70000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'database'),
  ('70000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', 'application')
ON CONFLICT (object_id, application_id, dependency_role) DO NOTHING;

INSERT INTO object_server_binding (object_id, server_id, binding_role)
VALUES
  ('70000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'runtime'),
  ('70000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 'runtime'),
  ('70000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', 'runtime')
ON CONFLICT (object_id, server_id, binding_role) DO NOTHING;

INSERT INTO data_source (id, code, name, source_type, env, base_url, health_check_path, auth_type, secret_ref, timeout_seconds, retry_count, rate_limit_qps, status, last_check_at, last_success_at, last_error_code, last_error_message)
VALUES
  ('71000000-0000-0000-0000-000000000001', 'ds-prom-prod', 'prometheus-prod', 'PROMETHEUS', 'prod', 'https://prometheus.example.com', '/api/v1/query', 'token', 'secret/prometheus-prod-token', 5, 2, 20, 'ENABLED', now() - interval '5 minutes', now() - interval '5 minutes', NULL, NULL),
  ('71000000-0000-0000-0000-000000000002', 'ds-loki-prod', 'loki-prod', 'LOKI', 'prod', 'https://loki.example.com', '/loki/api/v1/query_range', 'basic', 'secret/loki-prod-basic', 8, 1, 10, 'ENABLED', now() - interval '6 minutes', now() - interval '6 minutes', NULL, NULL),
  ('71000000-0000-0000-0000-000000000003', 'ds-agent-prod', 'agent-gateway-prod', 'AGENT', 'prod', 'https://agent-gateway.example.com', '/health', 'token', 'secret/agent-prod-token', 3, 1, 50, 'ENABLED', now() - interval '2 minutes', now() - interval '2 minutes', NULL, NULL),
  ('71000000-0000-0000-0000-000000000004', 'ds-kafka-prod', 'kafka-exporter-prod', 'KAFKA', 'prod', 'https://kafka-exporter.example.com', '/metrics', 'token', 'secret/kafka-prod-token', 5, 2, 20, 'ENABLED', now() - interval '5 minutes', now() - interval '5 minutes', NULL, NULL),
  ('71000000-0000-0000-0000-000000000005', 'ds-postgresql-prod', 'postgres-exporter-prod', 'POSTGRESQL', 'prod', 'https://postgres-exporter.example.com', '/metrics', 'token', 'secret/postgresql-prod-token', 5, 2, 20, 'ENABLED', now() - interval '5 minutes', now() - interval '5 minutes', NULL, NULL),
  ('71000000-0000-0000-0000-000000000006', 'ds-prom-pre', 'prometheus-pre', 'PROMETHEUS', 'pre', 'https://prometheus-pre.example.com', '/api/v1/query', 'token', 'secret/prometheus-pre-token', 5, 2, 15, 'UNHEALTHY', now() - interval '4 minutes', now() - interval '3 hours', 'CONNECT_TIMEOUT', 'Connection timed out')
ON CONFLICT (code, env) DO UPDATE
SET name = EXCLUDED.name,
    status = EXCLUDED.status,
    last_check_at = EXCLUDED.last_check_at,
    last_success_at = EXCLUDED.last_success_at,
    last_error_code = EXCLUDED.last_error_code,
    last_error_message = EXCLUDED.last_error_message,
    updated_at = now();

INSERT INTO data_source_binding (code, object_id, source_id, binding_type, external_labels, mapping_config, last_seen_at, access_status, failure_reason)
VALUES
  ('bind-kafka-metrics', '70000000-0000-0000-0000-000000000001', '71000000-0000-0000-0000-000000000001', 'METRIC', '{"job":"kafka-exporter","cluster":"orders"}', '{"metricPrefix":"kafka_"}', now() - interval '1 minute', 'CONNECTED', NULL),
  ('bind-kafka-logs', '70000000-0000-0000-0000-000000000001', '71000000-0000-0000-0000-000000000002', 'LOG', '{"app":"kafka","cluster":"orders"}', '{"index":"middleware-*"}', now() - interval '3 minutes', 'CONNECTED', NULL),
  ('bind-pg-metrics', '70000000-0000-0000-0000-000000000002', '71000000-0000-0000-0000-000000000001', 'METRIC', '{"job":"postgres-exporter","instance":"ace-pg"}', '{"metricPrefix":"pg_"}', now() - interval '1 minute', 'CONNECTED', NULL),
  ('bind-ipro-metrics', '70000000-0000-0000-0000-000000000003', '71000000-0000-0000-0000-000000000006', 'METRIC', '{"job":"ipro-api"}', '{"metricPrefix":"http_"}', now() - interval '3 hours', 'SOURCE_UNAVAILABLE', 'Prometheus pre is unavailable')
ON CONFLICT (code) DO UPDATE
SET external_labels = EXCLUDED.external_labels,
    mapping_config = EXCLUDED.mapping_config,
    last_seen_at = EXCLUDED.last_seen_at,
    access_status = EXCLUDED.access_status,
    failure_reason = EXCLUDED.failure_reason,
    updated_at = now();

INSERT INTO agent_instance (code, server_id, version, status, last_heartbeat_at, config_version, failure_reason)
VALUES
  ('agent-ace-1', '30000000-0000-0000-0000-000000000001', '1.8.2', 'ONLINE', now() - interval '35 seconds', 'cfg-20260426-01', NULL),
  ('agent-ace-2', '30000000-0000-0000-0000-000000000002', '1.8.2', 'ONLINE', now() - interval '42 seconds', 'cfg-20260426-01', NULL),
  ('agent-ipro-1', '30000000-0000-0000-0000-000000000003', '1.7.0', 'CONFIG_ERROR', now() - interval '4 minutes', 'cfg-20260420-02', 'Metric scrape config version is outdated'),
  ('agent-cms-1', '30000000-0000-0000-0000-000000000004', '1.8.1', 'NO_HEARTBEAT', now() - interval '18 minutes', 'cfg-20260425-01', 'No heartbeat for 18 minutes')
ON CONFLICT (code) DO UPDATE
SET version = EXCLUDED.version,
    status = EXCLUDED.status,
    last_heartbeat_at = EXCLUDED.last_heartbeat_at,
    config_version = EXCLUDED.config_version,
    failure_reason = EXCLUDED.failure_reason,
    updated_at = now();

INSERT INTO metric_definition (code, name, object_type, category, unit, source_type, default_query_template, labels)
VALUES
  ('broker_up', 'Kafka broker up', 'KAFKA', 'availability', 'count', 'PROMETHEUS', 'sum(kafka_brokers{cluster="$object"})', ARRAY['cluster']),
  ('mq_lag', 'MQ consumer lag', 'KAFKA', 'backlog', 'messages', 'PROMETHEUS', 'sum(kafka_consumergroup_lag{cluster="$object"})', ARRAY['cluster','consumer_group']),
  ('db_conn_usage', 'Database connection usage', 'POSTGRESQL', 'connection', '%', 'PROMETHEUS', 'pg_stat_activity_count / pg_settings_max_connections * 100', ARRAY['instance']),
  ('slow_sql_count', 'Slow SQL count', 'POSTGRESQL', 'sql', 'count', 'PROMETHEUS', 'increase(pg_slow_queries_total{instance="$object"}[5m])', ARRAY['instance']),
  ('http_5xx_rate', 'HTTP 5xx rate', 'APPLICATION', 'error', '%', 'PROMETHEUS', 'sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))', ARRAY['application','instance'])
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    default_query_template = EXCLUDED.default_query_template,
    labels = EXCLUDED.labels,
    updated_at = now();

INSERT INTO metric_series_mapping (code, object_type, metric_code, source_type, external_metric, query_template, unit, default_labels)
VALUES
  ('map-kafka-lag', 'KAFKA', 'mq_lag', 'PROMETHEUS', 'kafka_consumergroup_lag', 'sum by (cluster, consumergroup) (kafka_consumergroup_lag{cluster="$object"})', 'messages', '{"cluster":"$object"}'),
  ('map-kafka-up', 'KAFKA', 'broker_up', 'PROMETHEUS', 'kafka_brokers', 'sum(kafka_brokers{cluster="$object"})', 'count', '{"cluster":"$object"}'),
  ('map-pg-conn', 'POSTGRESQL', 'db_conn_usage', 'PROMETHEUS', 'pg_stat_activity_count', 'pg_stat_activity_count{instance="$object"} / pg_settings_max_connections{instance="$object"} * 100', '%', '{"instance":"$object"}'),
  ('map-pg-slow', 'POSTGRESQL', 'slow_sql_count', 'PROMETHEUS', 'pg_slow_queries_total', 'increase(pg_slow_queries_total{instance="$object"}[5m])', 'count', '{"instance":"$object"}')
ON CONFLICT (code) DO UPDATE
SET query_template = EXCLUDED.query_template,
    default_labels = EXCLUDED.default_labels,
    updated_at = now();

INSERT INTO log_entry_sample (code, occurred_at, application_id, object_id, env, level, message, trace_id, source_id, labels)
VALUES
  ('log-ace-slow-sql', now() - interval '12 minutes', '20000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000002', 'prod', 'WARN', 'Slow SQL detected: select * from orders where customer_phone=''***''', 'trace-ace-001', '71000000-0000-0000-0000-000000000002', '{"service":"ace-api","instance":"ace-api-02"}'),
  ('log-ace-kafka-lag', now() - interval '8 minutes', '20000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'prod', 'ERROR', 'Consumer lag exceeded threshold for group ace-order-worker', 'trace-ace-002', '71000000-0000-0000-0000-000000000002', '{"topic":"order-events","consumerGroup":"ace-order-worker"}'),
  ('log-ipro-kafka-lag', now() - interval '7 minutes', '20000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000001', 'prod', 'WARN', 'Shared Kafka backlog visible for authorized application summary', 'trace-ipro-001', '71000000-0000-0000-0000-000000000002', '{"topic":"order-events"}')
ON CONFLICT (code) DO UPDATE
SET occurred_at = EXCLUDED.occurred_at,
    message = EXCLUDED.message,
    labels = EXCLUDED.labels;
