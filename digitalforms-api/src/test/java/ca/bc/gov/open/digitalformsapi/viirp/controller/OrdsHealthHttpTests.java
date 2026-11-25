package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContext;

import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiClient;

class OrdsHealthHttpTests extends HttpApiTestSupport {

    @Autowired
    private ApiClient ordsClient;

    @Autowired
    private ApplicationContext context;

    @Test
    void publicOrdsHealthOnlyReportsStatusEvenWhenAuthenticated() throws Exception {
        for (String authorization : new String[] {null, AUTH}) {
            vips.enqueue(200, "{\"status\":\"success\",\"host\":\"private-db\",\"instance\":\"private-instance\"}");
            assertHealth("/actuator/health/ords", authorization, 200, "UP");
            checkedHealthCall();
        }
        responseJson(request("GET", "/actuator/health/ords/unknown", null, null), 401);
        responseJson(request("GET", "/actuator/health/diskSpace", null, null), 401);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{", "[]", "{}", "{\"status\":null}", "{\"status\":\"\"}",
            "{\"status\":\" \"}", "{\"status\":\"UP\"}", "{\"status\":\"failure\"}", "{\"status\":{}}"})
    void emptyMalformedOrUnsuccessfulResponsesAreDown(String body) throws Exception {
        vips.enqueue(200, body);
        assertHealth("/actuator/health/ords", null, 503, "DOWN");
        checkedHealthCall();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 404, 408, 429, 500, 502, 503, 504})
    void upstreamHttpFailuresAreDownWithoutExposingDetailsOrRetryHeaders(int upstreamStatus) throws Exception {
        vips.enqueue(upstreamStatus, "{\"error\":\"ORA-20011: private-db\"}",
                upstreamStatus == 408 ? Map.of() : Map.of("Retry-After", "120"));
        if (upstreamStatus == 408) {
            // Preserve the shared ORDS client's existing single HTTP-408 retry.
            vips.enqueue(upstreamStatus, "{\"error\":\"ORA-20011: private-db\"}");
        }
        assertHealth("/actuator/health/ords", null, 503, "DOWN");
        checkedHealthCall();
        if (upstreamStatus == 408) {
            checkedHealthCall();
        }
    }

    @Test
    void ordsOutageAffectsAggregateHealthButNeitherOpenShiftProbe() throws Exception {
        vips.enqueue(503, "{\"error\":\"unavailable\"}");
        var response = request("GET", "/actuator/health", null, null);
        assertEquals(503, response.statusCode());
        var body = json.readTree(response.body());
        assertEquals("DOWN", body.path("status").asString());
        assertTrue(Set.of("status", "groups").containsAll(body.propertyNames()), body.toString());
        assertEquals(3, body.path("groups").size());
        assertEquals(Set.of("ords", "liveness", "readiness"),
                Set.of(body.path("groups").get(0).asString(), body.path("groups").get(1).asString(),
                        body.path("groups").get(2).asString()));
        checkedHealthCall();
        for (String authorization : new String[] {null, AUTH}) {
            assertHealth("/actuator/health/liveness", authorization, 200, "UP");
            assertHealth("/actuator/health/readiness", authorization, 200, "UP");
        }
        // No queued responses: teardown detects any ORDS call made by a probe.
    }

    @Test
    void probesFollowApplicationAvailabilityWithoutContactingOrds() throws Exception {
        try {
            AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
            assertHealth("/actuator/health/readiness", null, 503, "OUT_OF_SERVICE");
            assertHealth("/actuator/health/liveness", null, 200, "UP");
            AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);
            assertHealth("/actuator/health/liveness", null, 503, "DOWN");
        } finally {
            AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
            AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        }
    }

    @Test
    void readTimeoutIsDownAndDoesNotBlockTheProbes() throws Exception {
        int timeout = ordsClient.getReadTimeout();
        try {
            ordsClient.setReadTimeout(200);
            vips.responses.add(new StubResponse(200, "{\"status\":\"success\"}".getBytes(StandardCharsets.UTF_8),
                    "application/json", Map.of(), 1000, 0));
            assertHealth("/actuator/health/ords", null, 503, "DOWN");
            checkedHealthCall();
            assertHealth("/actuator/health/liveness", null, 200, "UP");
            assertHealth("/actuator/health/readiness", null, 200, "UP");
        } finally {
            ordsClient.setReadTimeout(timeout);
        }
    }

    @Test
    void refusedConnectionAndInvalidLocalConfigurationAreDown() throws Exception {
        int port;
        try (var reservation = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = reservation.getLocalPort();
        }
        String basePath = ordsClient.getBasePath();
        try {
            ordsClient.setBasePath("http://127.0.0.1:" + port + "/ords");
            assertHealth("/actuator/health/ords", null, 503, "DOWN");
            ordsClient.setBasePath("not-a-url");
            assertHealth("/actuator/health/ords", null, 503, "DOWN");
        } finally {
            ordsClient.setBasePath(basePath);
        }
    }

    private void assertHealth(String path, String authorization, int status, String healthStatus) throws Exception {
        var response = request("GET", path, null, authorization);
        assertEquals(status, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertEquals(json.readTree("{\"status\":\"" + healthStatus + "\"}"), json.readTree(response.body()));
        assertTrue(response.headers().allValues("Retry-After").isEmpty());
    }

    private void checkedHealthCall() throws Exception {
        var upstream = vips.requests.poll(2, TimeUnit.SECONDS);
        assertNotNull(upstream);
        assertEquals("GET", upstream.method());
        assertEquals("/ords/health", upstream.uri().getPath());
    }
}
