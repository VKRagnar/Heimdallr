# Observability Test Stack

This stack starts Prometheus, Grafana, Elasticsearch, Logstash, Kibana, and a mock Agent Gateway for local or test-environment integration checks.

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
