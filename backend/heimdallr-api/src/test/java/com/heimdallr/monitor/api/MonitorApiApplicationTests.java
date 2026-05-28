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
        assertThat(sreResponse.body()).contains("\"id\":\"audit-001\"");
    }

    @Test
    void sprint1AssetMaintenanceWritesAuditEventsAndRejectsMissingPermission() throws Exception {
        int beforeAuditCount = auditEventCount();

        HttpResponse<String> createApplicationResponse = post(
                "/api/v1/applications",
                "admin-token",
                "{\"id\":\"app-sprint1-assets\",\"code\":\"S1A\",\"name\":\"Sprint 1 Assets\",\"businessLine\":\"asset-hardening\",\"environment\":\"prod\",\"ownerUserIds\":[\"u-admin\"],\"accessStatus\":\"CONNECTED\"}"
        );
        assertThat(createApplicationResponse.statusCode()).isEqualTo(200);
        assertThat(createApplicationResponse.body()).contains("\"id\":\"app-sprint1-assets\"");

        HttpResponse<String> updateApplicationResponse = put(
                "/api/v1/applications/app-sprint1-assets",
                "admin-token",
                "{\"code\":\"S1A\",\"name\":\"Sprint 1 Assets Updated\",\"businessLine\":\"asset-hardening\",\"environment\":\"prod\",\"ownerUserIds\":[\"u-admin\"],\"accessStatus\":\"DEGRADED\"}"
        );
        assertThat(updateApplicationResponse.statusCode()).isEqualTo(200);
        assertThat(updateApplicationResponse.body()).contains("Sprint 1 Assets Updated");
        assertThat(updateApplicationResponse.body()).contains("\"accessStatus\":\"DEGRADED\"");

        HttpResponse<String> importServerResponse = post(
                "/api/v1/servers/import",
                "admin-token",
                "[{\"id\":\"srv-sprint1-assets\",\"hostname\":\"sprint1-api-01\",\"ip\":\"10.10.88.11\",\"environment\":\"prod\",\"applicationIds\":[\"app-sprint1-assets\"],\"accessStatus\":\"CONNECTED\"}]"
        );
        assertThat(importServerResponse.statusCode()).isEqualTo(200);
        assertThat(importServerResponse.body()).contains("\"total\":1");
        assertThat(importServerResponse.body()).contains("\"id\":\"srv-sprint1-assets\"");

        HttpResponse<String> forbiddenResponse = post(
                "/api/v1/applications",
                "ace-owner-token",
                "{\"id\":\"app-denied\",\"code\":\"DENIED\",\"name\":\"Denied\",\"businessLine\":\"trade\",\"environment\":\"prod\",\"ownerUserIds\":[],\"accessStatus\":\"CONNECTED\"}"
        );
        assertThat(forbiddenResponse.statusCode()).isEqualTo(403);
        assertThat(forbiddenResponse.body()).contains("Missing permission: applications:write");

        HttpResponse<String> auditResponse = get("/api/v1/system/audit-events", "admin-token", null);
        assertThat(countOccurrences(auditResponse.body(), "\"id\":\"audit-")).isGreaterThanOrEqualTo(beforeAuditCount + 3);
        assertThat(auditResponse.body()).contains("\"actorUserId\":\"u-admin\"");
        assertThat(auditResponse.body()).contains("\"action\":\"APPLICATION_UPSERT\"");
        assertThat(auditResponse.body()).contains("\"targetType\":\"APPLICATION\"");
        assertThat(auditResponse.body()).contains("\"result\":\"SUCCESS\"");
        assertThat(auditResponse.body()).contains("\"targetId\":\"srv-sprint1-assets\"");
        assertThat(auditResponse.body()).contains("\"targetType\":\"SERVER\"");
        assertThat(auditResponse.body().indexOf("\"id\":\"audit-001\"")).isLessThan(auditResponse.body().indexOf("\"id\":\"audit-002\""));
        assertThat(auditResponse.body().indexOf("\"id\":\"audit-002\"")).isLessThan(auditResponse.body().indexOf("\"id\":\"audit-003\""));
    }

    @Test
    void sprint1AccessGrantAndRevokeAffectApplicationVisibilityAndAudit() throws Exception {
        post(
                "/api/v1/applications",
                "admin-token",
                "{\"id\":\"app-sprint1-access\",\"code\":\"S1ACCESS\",\"name\":\"Sprint 1 Access\",\"businessLine\":\"restricted-sprint1\",\"environment\":\"prod\",\"ownerUserIds\":[],\"accessStatus\":\"CONNECTED\"}"
        );
        HttpResponse<String> beforeGrantResponse = get("/api/v1/applications", "ace-owner-token", null);
        assertThat(beforeGrantResponse.body()).doesNotContain("\"id\":\"app-sprint1-access\"");

        int beforeAuditCount = auditEventCount();
        HttpResponse<String> grantResponse = post(
                "/api/v1/access/users/u-ace-owner/applications",
                "admin-token",
                "{\"applicationId\":\"app-sprint1-access\"}"
        );
        assertThat(grantResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> afterGrantResponse = get("/api/v1/applications", "ace-owner-token", null);
        assertThat(afterGrantResponse.body()).contains("\"id\":\"app-sprint1-access\"");

        HttpResponse<String> revokeResponse = delete("/api/v1/access/users/u-ace-owner/applications/app-sprint1-access", "admin-token");
        assertThat(revokeResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> afterRevokeResponse = get("/api/v1/applications", "ace-owner-token", null);
        assertThat(afterRevokeResponse.body()).doesNotContain("\"id\":\"app-sprint1-access\"");

        HttpResponse<String> forbiddenGrantResponse = post(
                "/api/v1/access/users/u-ace-owner/business-lines",
                "ace-owner-token",
                "{\"businessLine\":\"content\"}"
        );
        assertThat(forbiddenGrantResponse.statusCode()).isEqualTo(403);
        assertThat(forbiddenGrantResponse.body()).contains("Missing permission: access:write");

        HttpResponse<String> auditResponse = get("/api/v1/system/audit-events", "admin-token", null);
        assertThat(countOccurrences(auditResponse.body(), "\"id\":\"audit-")).isGreaterThanOrEqualTo(beforeAuditCount + 2);
        assertThat(auditResponse.body()).contains("\"action\":\"ACCESS_GRANT_APPLICATION\"");
        assertThat(auditResponse.body()).contains("\"action\":\"ACCESS_REVOKE_APPLICATION\"");
    }

    @Test
    void sprint1WriteApisValidateRequestsAndReturnExpectedBoundaryErrors() throws Exception {
        HttpResponse<String> invalidApplicationResponse = post(
                "/api/v1/applications",
                "admin-token",
                "{\"id\":\"app-invalid\",\"code\":\"\",\"name\":\"Invalid\",\"businessLine\":\"qa\",\"environment\":\"prod\",\"ownerUserIds\":[],\"accessStatus\":\"CONNECTED\"}"
        );
        assertThat(invalidApplicationResponse.statusCode()).isEqualTo(400);
        assertThat(invalidApplicationResponse.body()).contains("\"code\":\"VALIDATION_FAILED\"");

        HttpResponse<String> invalidServerResponse = post(
                "/api/v1/servers",
                "admin-token",
                "{\"id\":\"srv-invalid\",\"hostname\":\"invalid-01\",\"ip\":\"10.10.77.1\",\"environment\":\"prod\",\"applicationIds\":[],\"accessStatus\":\"CONNECTED\"}"
        );
        assertThat(invalidServerResponse.statusCode()).isEqualTo(400);
        assertThat(invalidServerResponse.body()).contains("\"code\":\"VALIDATION_FAILED\"");

        HttpResponse<String> emptyImportResponse = post("/api/v1/applications/import", "admin-token", "[]");
        assertThat(emptyImportResponse.statusCode()).isEqualTo(400);
        assertThat(emptyImportResponse.body()).contains("\"code\":\"VALIDATION_FAILED\"");

        HttpResponse<String> unknownApplicationResponse = post(
                "/api/v1/servers/import",
                "admin-token",
                "[{\"id\":\"srv-unknown-app\",\"hostname\":\"unknown-app-01\",\"ip\":\"10.10.77.2\",\"environment\":\"prod\",\"applicationIds\":[\"app-not-found\"],\"accessStatus\":\"CONNECTED\"}]"
        );
        assertThat(unknownApplicationResponse.statusCode()).isEqualTo(404);
        assertThat(unknownApplicationResponse.body()).contains("Application not found");

        HttpResponse<String> unknownUserGrantResponse = post(
                "/api/v1/access/users/u-missing/applications",
                "admin-token",
                "{\"applicationId\":\"app-ace\"}"
        );
        assertThat(unknownUserGrantResponse.statusCode()).isEqualTo(404);
        assertThat(unknownUserGrantResponse.body()).contains("User not found");
    }

    @Test
    void sprint1BusinessLineGrantAndRevokeControlVisibility() throws Exception {
        HttpResponse<String> beforeGrantResponse = get("/api/v1/applications", "ace-owner-token", null);
        assertThat(beforeGrantResponse.body()).doesNotContain("\"id\":\"app-cms\"");

        int beforeAuditCount = auditEventCount();
        HttpResponse<String> grantResponse = post(
                "/api/v1/access/users/u-ace-owner/business-lines",
                "admin-token",
                "{\"businessLine\":\"content\"}"
        );
        assertThat(grantResponse.statusCode()).isEqualTo(200);
        assertThat(grantResponse.body()).contains("\"businessLines\"");
        assertThat(grantResponse.body()).contains("content");

        HttpResponse<String> afterGrantResponse = get("/api/v1/applications", "ace-owner-token", null);
        assertThat(afterGrantResponse.body()).contains("\"id\":\"app-cms\"");

        HttpResponse<String> revokeResponse = delete("/api/v1/access/users/u-ace-owner/business-lines/content", "admin-token");
        assertThat(revokeResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> afterRevokeResponse = get("/api/v1/applications", "ace-owner-token", null);
        assertThat(afterRevokeResponse.body()).doesNotContain("\"id\":\"app-cms\"");

        HttpResponse<String> auditResponse = get("/api/v1/system/audit-events", "admin-token", null);
        assertThat(countOccurrences(auditResponse.body(), "\"id\":\"audit-")).isGreaterThanOrEqualTo(beforeAuditCount + 2);
        assertThat(auditResponse.body()).contains("\"action\":\"ACCESS_GRANT_BUSINESS_LINE\"");
        assertThat(auditResponse.body()).contains("\"action\":\"ACCESS_REVOKE_BUSINESS_LINE\"");
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

    @Test
    void sprint3AlertRuleEvaluationStateTransitionsAndNotificationRecordsWork() throws Exception {
        HttpResponse<String> createRuleResponse = post(
                "/api/v1/alerts/rules",
                "admin-token",
                "{\"name\":\"Kafka lag smoke\",\"objectId\":\"obj-kafka-orders\",\"metricCode\":\"mq_lag\",\"operator\":\">\",\"threshold\":1000,\"windowSeconds\":300,\"durationSeconds\":60,\"evaluationIntervalSeconds\":60,\"severity\":\"P1\",\"enabled\":false,\"onCallGroupId\":\"trade-oncall\"}"
        );

        assertThat(createRuleResponse.statusCode()).isEqualTo(200);
        assertThat(createRuleResponse.body()).contains("\"name\":\"Kafka lag smoke\"");
        String ruleId = extractJsonString(createRuleResponse.body(), "id");

        HttpResponse<String> enableResponse = post("/api/v1/alerts/rules/" + ruleId + "/enable", "admin-token", "{}");
        assertThat(enableResponse.statusCode()).isEqualTo(200);
        assertThat(enableResponse.body()).contains("\"enabled\":true");

        HttpResponse<String> filteredRulesResponse = get("/api/v1/alerts/rules?enabled=true&severity=P1&keyword=Kafka", "admin-token", null);
        assertThat(filteredRulesResponse.statusCode()).isEqualTo(200);
        assertThat(filteredRulesResponse.body()).contains("\"id\":\"" + ruleId + "\"");

        HttpResponse<String> evaluateResponse = post("/api/v1/alerts/rules/" + ruleId + "/evaluate", "admin-token", "{}");
        assertThat(evaluateResponse.statusCode()).isEqualTo(200);
        assertThat(evaluateResponse.body()).contains("\"ruleId\":\"" + ruleId + "\"");
        assertThat(evaluateResponse.body()).contains("\"status\":\"NOTIFIED\"");
        String eventId = extractJsonString(evaluateResponse.body(), "id");

        HttpResponse<String> filteredEventsResponse = get("/api/v1/alerts/events?status=notified&severity=P1&keyword=Kafka", "admin-token", null);
        assertThat(filteredEventsResponse.statusCode()).isEqualTo(200);
        assertThat(filteredEventsResponse.body()).contains("\"id\":\"" + eventId + "\"");

        HttpResponse<String> runtimeResponse = get("/api/v1/alerts/rules/" + ruleId + "/runtime", "admin-token", null);
        assertThat(runtimeResponse.statusCode()).isEqualTo(200);
        assertThat(runtimeResponse.body()).contains("\"ruleId\":\"" + ruleId + "\"");
        assertThat(runtimeResponse.body()).contains("\"lastStatus\":\"MATCHED\"");
        assertThat(runtimeResponse.body()).contains("\"lastValue\":");
        assertThat(runtimeResponse.body()).contains("\"nextEvaluateAt\"");

        HttpResponse<String> samplesResponse = get("/api/v1/alerts/rules/" + ruleId + "/samples", "admin-token", null);
        assertThat(samplesResponse.statusCode()).isEqualTo(200);
        assertThat(samplesResponse.body()).contains("\"total\":1");
        assertThat(samplesResponse.body()).contains("\"status\":\"MATCHED\"");
        assertThat(samplesResponse.body()).contains("\"matched\":true");
        assertThat(samplesResponse.body()).contains("\"eventId\":\"" + eventId + "\"");

        HttpResponse<String> notificationsResponse = get("/api/v1/alerts/notifications?eventId=" + eventId, "admin-token", null);
        assertThat(notificationsResponse.statusCode()).isEqualTo(200);
        assertThat(notificationsResponse.body()).contains("\"eventId\":\"" + eventId + "\"");
        assertThat(notificationsResponse.body()).contains("\"status\":\"SENT\"");

        HttpResponse<String> duplicateEvaluateResponse = post("/api/v1/alerts/rules/" + ruleId + "/evaluate", "admin-token", "{}");
        assertThat(duplicateEvaluateResponse.statusCode()).isEqualTo(200);
        assertThat(duplicateEvaluateResponse.body()).contains("\"id\":\"" + eventId + "\"");
        assertThat(duplicateEvaluateResponse.body()).contains("\"status\":\"NOTIFIED\"");

        HttpResponse<String> acknowledgeResponse = post(
                "/api/v1/alerts/events/" + eventId + "/actions",
                "admin-token",
                "{\"action\":\"ACKNOWLEDGE\",\"message\":\"ack\"}"
        );
        assertThat(acknowledgeResponse.statusCode()).isEqualTo(200);
        assertThat(acknowledgeResponse.body()).contains("\"status\":\"ACKNOWLEDGED\"");

        HttpResponse<String> processResponse = post(
                "/api/v1/alerts/events/" + eventId + "/actions",
                "admin-token",
                "{\"action\":\"PROCESS\",\"message\":\"checking consumer\"}"
        );
        assertThat(processResponse.statusCode()).isEqualTo(200);
        assertThat(processResponse.body()).contains("\"status\":\"PROCESSING\"");

        HttpResponse<String> closeResponse = post(
                "/api/v1/alerts/events/" + eventId + "/actions",
                "admin-token",
                "{\"action\":\"CLOSE\",\"message\":\"consumer restarted\"}"
        );
        assertThat(closeResponse.statusCode()).isEqualTo(200);
        assertThat(closeResponse.body()).contains("\"status\":\"CLOSED\"");

        HttpResponse<String> illegalAcknowledgeResponse = post(
                "/api/v1/alerts/events/" + eventId + "/actions",
                "admin-token",
                "{\"action\":\"ACKNOWLEDGE\",\"message\":\"late ack\"}"
        );
        assertThat(illegalAcknowledgeResponse.statusCode()).isEqualTo(400);
        assertThat(illegalAcknowledgeResponse.body()).contains("Alert event is already terminal");

        HttpResponse<String> historyResponse = get("/api/v1/alerts/events/" + eventId + "/history", "admin-token", null);
        assertThat(historyResponse.statusCode()).isEqualTo(200);
        assertThat(historyResponse.body()).contains("\"action\":\"TRIGGER\"");
        assertThat(historyResponse.body()).contains("\"action\":\"CLOSE\"");
    }

    @Test
    void sprint3AlertViewerCannotMutateRulesOrEvents() throws Exception {
        HttpResponse<String> createRuleResponse = post(
                "/api/v1/alerts/rules",
                "admin-token",
                kafkaRuleBody("Kafka viewer permission smoke", 1000, true)
        );
        assertThat(createRuleResponse.statusCode()).isEqualTo(200);
        String ruleId = extractJsonString(createRuleResponse.body(), "id");

        HttpResponse<String> evaluateResponse = post("/api/v1/alerts/rules/" + ruleId + "/evaluate", "admin-token", "{}");
        assertThat(evaluateResponse.statusCode()).isEqualTo(200);
        String eventId = extractJsonString(evaluateResponse.body(), "id");

        HttpResponse<String> viewerRulesResponse = get("/api/v1/alerts/rules?keyword=viewer", "alert-viewer-token", null);
        assertThat(viewerRulesResponse.statusCode()).isEqualTo(200);
        assertThat(viewerRulesResponse.body()).contains("\"id\":\"" + ruleId + "\"");

        assertMissingAlertWrite(post("/api/v1/alerts/rules", "alert-viewer-token", kafkaRuleBody("viewer denied create", 1000, false)));
        assertMissingAlertWrite(put("/api/v1/alerts/rules/" + ruleId, "alert-viewer-token", kafkaRuleBody("viewer denied update", 1000, false)));
        assertMissingAlertWrite(post("/api/v1/alerts/rules/" + ruleId + "/enable", "alert-viewer-token", "{}"));
        assertMissingAlertWrite(post("/api/v1/alerts/rules/" + ruleId + "/disable", "alert-viewer-token", "{}"));
        assertMissingAlertWrite(post("/api/v1/alerts/rules/" + ruleId + "/evaluate", "alert-viewer-token", "{}"));
        assertMissingAlertWrite(post("/api/v1/alerts/rules/evaluate", "alert-viewer-token", "{}"));
        assertMissingAlertWrite(post("/api/v1/alerts/rules/evaluate-due", "alert-viewer-token", "{}"));
        assertMissingAlertWrite(post("/api/v1/alerts/notifications/retry-due", "alert-viewer-token", "{}"));
        assertMissingAlertWrite(post(
                "/api/v1/alerts/events/" + eventId + "/actions",
                "alert-viewer-token",
                "{\"action\":\"ACKNOWLEDGE\",\"message\":\"viewer should not ack\"}"
        ));
    }

    @Test
    void sprint3AlertAccessIsFilteredByUserScope() throws Exception {
        HttpResponse<String> createRuleResponse = post(
                "/api/v1/alerts/rules",
                "admin-token",
                "{\"name\":\"CMS redis scope smoke\",\"objectId\":\"obj-redis-cms\",\"metricCode\":\"redis_memory_usage\",\"operator\":\">\",\"threshold\":50,\"windowSeconds\":300,\"durationSeconds\":60,\"evaluationIntervalSeconds\":60,\"severity\":\"P2\",\"enabled\":true,\"onCallGroupId\":null}"
        );
        assertThat(createRuleResponse.statusCode()).isEqualTo(200);
        String hiddenRuleId = extractJsonString(createRuleResponse.body(), "id");

        HttpResponse<String> evaluateResponse = post("/api/v1/alerts/rules/" + hiddenRuleId + "/evaluate", "admin-token", "{}");
        assertThat(evaluateResponse.statusCode()).isEqualTo(200);
        String hiddenEventId = extractJsonString(evaluateResponse.body(), "id");

        HttpResponse<String> visibleRulesResponse = get("/api/v1/alerts/rules?keyword=CMS", "ace-owner-token", null);
        assertThat(visibleRulesResponse.statusCode()).isEqualTo(200);
        assertThat(visibleRulesResponse.body()).doesNotContain("\"id\":\"" + hiddenRuleId + "\"");

        HttpResponse<String> visibleEventsResponse = get("/api/v1/alerts/events?keyword=CMS", "ace-owner-token", null);
        assertThat(visibleEventsResponse.statusCode()).isEqualTo(200);
        assertThat(visibleEventsResponse.body()).doesNotContain("\"id\":\"" + hiddenEventId + "\"");

        assertAlertNotFound(get("/api/v1/alerts/rules/" + hiddenRuleId, "ace-owner-token", null));
        assertAlertNotFound(get("/api/v1/alerts/rules/" + hiddenRuleId + "/runtime", "ace-owner-token", null));
        assertAlertNotFound(get("/api/v1/alerts/rules/" + hiddenRuleId + "/samples", "ace-owner-token", null));
        assertAlertNotFound(post("/api/v1/alerts/rules/" + hiddenRuleId + "/enable", "ace-owner-token", "{}"));
        assertAlertNotFound(post("/api/v1/alerts/rules/" + hiddenRuleId + "/evaluate", "ace-owner-token", "{}"));
        assertAlertNotFound(put("/api/v1/alerts/rules/" + hiddenRuleId, "ace-owner-token", kafkaRuleBody("cross scope overwrite attempt", 1000, true)));
        assertAlertNotFound(post(
                "/api/v1/alerts/events/" + hiddenEventId + "/actions",
                "ace-owner-token",
                "{\"action\":\"ACKNOWLEDGE\",\"message\":\"cross scope should not ack\"}"
        ));
        assertAlertNotFound(get("/api/v1/alerts/events/" + hiddenEventId + "/history", "ace-owner-token", null));

        HttpResponse<String> hiddenNotificationsResponse = get("/api/v1/alerts/notifications?eventId=" + hiddenEventId, "ace-owner-token", null);
        assertThat(hiddenNotificationsResponse.statusCode()).isEqualTo(200);
        assertThat(hiddenNotificationsResponse.body()).contains("\"total\":0");
        assertThat(hiddenNotificationsResponse.body()).doesNotContain("\"eventId\":\"" + hiddenEventId + "\"");
    }

    @Test
    void unsupportedAlertRuleDeleteReturnsMethodNotAllowed() throws Exception {
        HttpResponse<String> createRuleResponse = post(
                "/api/v1/alerts/rules",
                "admin-token",
                kafkaRuleBody("Kafka method guard smoke", 1000, true)
        );
        assertThat(createRuleResponse.statusCode()).isEqualTo(200);
        String ruleId = extractJsonString(createRuleResponse.body(), "id");

        HttpResponse<String> deleteResponse = delete("/api/v1/alerts/rules/" + ruleId, "admin-token");

        assertThat(deleteResponse.statusCode()).isEqualTo(405);
        assertThat(deleteResponse.body()).contains("\"code\":\"BAD_REQUEST\"");
        assertThat(deleteResponse.body()).contains("Method not allowed");
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

    private HttpResponse<String> put(String path, String token, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path, String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .DELETE()
                .header("Authorization", token)
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private int auditEventCount() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/v1/system/audit-events", "admin-token", null);
        assertThat(response.statusCode()).isEqualTo(200);
        return countOccurrences(response.body(), "\"id\":\"audit-");
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static String extractJsonString(String json, String fieldName) {
        String needle = "\"" + fieldName + "\":\"";
        int start = json.indexOf(needle);
        assertThat(start).isGreaterThanOrEqualTo(0);
        int valueStart = start + needle.length();
        int valueEnd = json.indexOf("\"", valueStart);
        assertThat(valueEnd).isGreaterThan(valueStart);
        return json.substring(valueStart, valueEnd);
    }

    private static String kafkaRuleBody(String name, int threshold, boolean enabled) {
        return "{\"name\":\"" + name + "\",\"objectId\":\"obj-kafka-orders\",\"metricCode\":\"mq_lag\",\"operator\":\">\",\"threshold\":"
                + threshold
                + ",\"windowSeconds\":300,\"durationSeconds\":60,\"evaluationIntervalSeconds\":60,\"severity\":\"P1\",\"enabled\":"
                + enabled
                + ",\"onCallGroupId\":\"trade-oncall\"}";
    }

    private static void assertMissingAlertWrite(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("Missing permission: alerts:write");
    }

    private static void assertAlertNotFound(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("Alert");
        assertThat(response.body()).contains("not found");
    }
}
