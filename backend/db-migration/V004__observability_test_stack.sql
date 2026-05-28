UPDATE data_source
SET base_url = 'http://192.168.3.2:9090',
    health_check_path = '/-/ready',
    auth_type = 'none',
    secret_ref = NULL,
    status = 'ENABLED',
    last_check_at = now(),
    last_success_at = now(),
    last_error_code = NULL,
    last_error_message = NULL,
    updated_at = now()
WHERE code = 'ds-prom-prod' AND env = 'prod';

UPDATE data_source
SET name = 'elasticsearch-test',
    source_type = 'ELASTICSEARCH',
    base_url = 'http://192.168.3.2:9200',
    health_check_path = '/_cluster/health',
    auth_type = 'none',
    secret_ref = NULL,
    status = 'ENABLED',
    last_check_at = now(),
    last_success_at = now(),
    last_error_code = NULL,
    last_error_message = NULL,
    updated_at = now()
WHERE code = 'ds-loki-prod' AND env = 'prod';

UPDATE data_source
SET base_url = 'http://192.168.3.2:18080',
    health_check_path = '/health',
    auth_type = 'none',
    secret_ref = NULL,
    status = 'ENABLED',
    last_check_at = now(),
    last_success_at = now(),
    last_error_code = NULL,
    last_error_message = NULL,
    updated_at = now()
WHERE code = 'ds-agent-prod' AND env = 'prod';

UPDATE data_source_binding
SET mapping_config = '{"index":"test-logs-*","applicationField":"application","environmentField":"environment","traceField":"traceId"}',
    external_labels = '{"index":"test-logs-*"}',
    access_status = 'CONNECTED',
    failure_reason = NULL,
    updated_at = now()
WHERE source_id = '71000000-0000-0000-0000-000000000002';

UPDATE metric_series_mapping
SET query_template = 'agent_gateway_agents_online',
    external_metric = 'agent_gateway_agents_online',
    default_labels = '{}'::jsonb,
    updated_at = now()
WHERE code = 'map-kafka-lag';

UPDATE metric_series_mapping
SET query_template = 'up{job="agent-gateway"}',
    external_metric = 'up',
    default_labels = '{"job":"agent-gateway"}'::jsonb,
    updated_at = now()
WHERE code = 'map-kafka-up';

UPDATE metric_series_mapping
SET query_template = 'agent_gateway_heartbeat_latency_seconds',
    external_metric = 'agent_gateway_heartbeat_latency_seconds',
    default_labels = '{}'::jsonb,
    updated_at = now()
WHERE code IN ('map-pg-conn', 'map-pg-slow');
