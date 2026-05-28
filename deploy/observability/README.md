# Observability Test Stack

This stack starts Prometheus, Grafana, Elasticsearch, Logstash, Kibana, and a mock Agent Gateway for local or test-environment integration checks.

## Heimdallr API Scrape Target

Prometheus must scrape the Heimdallr API on the machine where the backend is running, not the machine where the observability stack is running. If the backend runs on a Windows developer host, use that host's LAN address in `deploy/observability/prometheus/prometheus.yml`.

For the current shared test setup:

- Observability stack: `192.168.3.2`
- Heimdallr API developer host: `192.168.3.11`
- Prometheus target: `192.168.3.11:8080`

After changing the mounted `prometheus.yml`, reload Prometheus:

```powershell
Invoke-RestMethod -Method Post http://192.168.3.2:9090/-/reload
```

Then verify the running stack is using the expected target:

```powershell
.\scripts\verify-observability-stack.ps1
```

If the script reports that `heimdallr-api-local` still scrapes `host.docker.internal:8080`, the running Prometheus container is not using this repository's `deploy/observability/prometheus/prometheus.yml`. Update the actual mounted config file on the host that runs the stack, then reload or restart Prometheus.

## Start

```powershell
Copy-Item deploy\.env.observability-test.example deploy\.env.observability-test
docker compose --env-file deploy\.env.observability-test -f deploy\docker-compose.observability-test.yml up -d
```

## Endpoints

| Service | URL |
| --- | --- |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 |
| Elasticsearch | http://localhost:9200 |
| Logstash HTTP input | http://localhost:8081 |
| Kibana | http://localhost:5601 |
| Agent Gateway mock | http://localhost:18080 |

Default Grafana login is `admin` / `admin`.

## Smoke Tests

```powershell
Invoke-RestMethod http://localhost:18080/health
Invoke-RestMethod http://localhost:9200/_cluster/health
Invoke-RestMethod -Method Post -ContentType "application/json" -Body '{"level":"INFO","service":"heimdallr-test","message":"hello elk","environment":"test"}' http://localhost:8081
```

Grafana provisions `Prometheus-Test` and `Elasticsearch-Test` automatically.
