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
}
