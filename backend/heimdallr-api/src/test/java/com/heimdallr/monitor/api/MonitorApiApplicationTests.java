package com.heimdallr.monitor.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MonitorApiApplicationTests {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void meReturnsCurrentUserAndRequestId() throws Exception {
        HttpResponse<String> response = get("/api/v1/me", "Bearer ace-owner-token", "req-test-me");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Request-Id")).contains("req-test-me");
        assertThat(response.body()).contains("\"code\":\"OK\"");
        assertThat(response.body()).contains("\"requestId\":\"req-test-me\"");
        assertThat(response.body()).contains("\"username\":\"ace-owner\"");
        assertThat(response.body()).contains("\"code\":\"APP_OWNER\"");
    }

    @Test
    void applicationListIsFilteredByCurrentUserScope() throws Exception {
        HttpResponse<String> listResponse = get("/api/v1/applications", "ace-owner-token", null);

        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.body()).contains("\"total\":1");
        assertThat(listResponse.body()).contains("\"id\":\"app-ace\"");
        assertThat(listResponse.body()).doesNotContain("\"id\":\"app-cms\"");

        HttpResponse<String> detailResponse = get("/api/v1/applications/app-cms", "ace-owner-token", null);

        assertThat(detailResponse.statusCode()).isEqualTo(404);
        assertThat(detailResponse.body()).contains("\"code\":\"NOT_FOUND\"");
        assertThat(detailResponse.body()).contains("Application not found");
    }

    @Test
    void auditEventsRequireAuditPermission() throws Exception {
        HttpResponse<String> forbiddenResponse = get("/api/v1/system/audit-events", "ace-owner-token", null);

        assertThat(forbiddenResponse.statusCode()).isEqualTo(403);
        assertThat(forbiddenResponse.body()).contains("\"code\":\"FORBIDDEN\"");

        HttpResponse<String> sreResponse = get("/api/v1/system/audit-events", "sre-token", null);

        assertThat(sreResponse.statusCode()).isEqualTo(200);
        assertThat(sreResponse.body()).contains("\"total\":3");
        assertThat(sreResponse.body()).contains("\"id\":\"audit-001\"");
    }

    @Test
    void sprint2DataSourcesReturnSafeDtosAndValidation() throws Exception {
        HttpResponse<String> listResponse = get("/api/v1/data-sources", "sre-token", null);

        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.body()).contains("\"success\":true");
        assertThat(listResponse.body()).contains("\"id\":\"ds-prom-prod\"");
        assertThat(listResponse.body()).contains("\"hasSecretRef\":true");
        assertThat(listResponse.body()).doesNotContain("secret/prometheus-prod-token");
        assertThat(listResponse.body()).doesNotContain("\"secretRef\"");

        HttpResponse<String> validationResponse = post("/api/v1/data-sources/ds-prom-prod/validate", "sre-token", "{}");

        assertThat(validationResponse.statusCode()).isEqualTo(200);
        assertThat(validationResponse.body()).contains("\"status\":\"PASSED\"");
        assertThat(validationResponse.body()).contains("\"name\":\"connectivity\"");
    }

    @Test
    void sprint2AccessMetricsLogsAndAgentsAreQueryable() throws Exception {
        HttpResponse<String> accessResponse = get("/api/v1/data-sources/access-status", "admin-token", null);

        assertThat(accessResponse.statusCode()).isEqualTo(200);
        assertThat(accessResponse.body()).contains("\"appId\":\"app-ipro\"");
        assertThat(accessResponse.body()).contains("\"metricsAccess\"");
        assertThat(accessResponse.body()).contains("\"agentStatus\"");

        HttpResponse<String> mappingsResponse = get("/api/v1/metrics/default-mappings?objectType=KAFKA", "admin-token", null);

        assertThat(mappingsResponse.statusCode()).isEqualTo(200);
        assertThat(mappingsResponse.body()).contains("\"metricCode\":\"mq_lag\"");
        assertThat(mappingsResponse.body()).contains("\"externalMetric\":\"kafka_consumergroup_lag\"");

        HttpResponse<String> metricResponse = post("/api/v1/metrics/query", "admin-token", "{\"metricCode\":\"mq_lag\",\"objectId\":\"obj-kafka-orders\"}");

        assertThat(metricResponse.statusCode()).isEqualTo(200);
        assertThat(metricResponse.body()).contains("\"metricCode\":\"mq_lag\"");
        assertThat(metricResponse.body()).contains("\"objectId\":\"obj-kafka-orders\"");

        HttpResponse<String> logResponse = post("/api/v1/logs/search", "admin-token", "{\"traceId\":\"trace-ipro-001\",\"pageNo\":1,\"pageSize\":5}");

        assertThat(logResponse.statusCode()).isEqualTo(200);
        assertThat(logResponse.body()).contains("\"traceId\":\"trace-ipro-001\"");
        assertThat(logResponse.body()).doesNotContain("\"traceId\":\"trace-ace-001\"");

        HttpResponse<String> agentsResponse = get("/api/v1/agents", "admin-token", null);

        assertThat(agentsResponse.statusCode()).isEqualTo(200);
        assertThat(agentsResponse.body()).contains("\"status\":\"CONFIG_ERROR\"");
    }

    private HttpResponse<String> get(String path, String token, String requestId) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .header("Authorization", token);
        if (requestId != null) {
            builder.header("X-Request-Id", requestId);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String token, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
