CREATE TABLE IF NOT EXISTS alert_rule_runtime (
  rule_id uuid PRIMARY KEY REFERENCES alert_rule(id) ON DELETE CASCADE,
  last_evaluated_at timestamptz,
  next_evaluate_at timestamptz,
  last_status varchar(32) NOT NULL DEFAULT 'pending',
  last_value numeric(18, 4),
  last_error text,
  evaluation_duration_ms bigint,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE TABLE IF NOT EXISTS alert_evaluation_sample (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  rule_id uuid NOT NULL REFERENCES alert_rule(id) ON DELETE CASCADE,
  evaluated_at timestamptz NOT NULL DEFAULT now(),
  status varchar(32) NOT NULL,
  metric_value numeric(18, 4),
  threshold numeric(18, 4) NOT NULL,
  operator varchar(16) NOT NULL,
  matched boolean NOT NULL DEFAULT false,
  event_id uuid REFERENCES alert_event(id) ON DELETE SET NULL,
  error text,
  evaluation_duration_ms bigint,
  extra jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT ck_alert_evaluation_sample_status CHECK (status IN ('matched', 'recovered', 'normal', 'no_data', 'failed'))
);

CREATE INDEX IF NOT EXISTS idx_alert_rule_runtime_due
  ON alert_rule_runtime (next_evaluate_at, last_evaluated_at)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_alert_evaluation_sample_rule_time
  ON alert_evaluation_sample (rule_id, evaluated_at DESC)
  WHERE deleted_at IS NULL;

CREATE TRIGGER trg_alert_rule_runtime_updated_at BEFORE UPDATE ON alert_rule_runtime FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_alert_evaluation_sample_updated_at BEFORE UPDATE ON alert_evaluation_sample FOR EACH ROW EXECUTE FUNCTION set_updated_at();
