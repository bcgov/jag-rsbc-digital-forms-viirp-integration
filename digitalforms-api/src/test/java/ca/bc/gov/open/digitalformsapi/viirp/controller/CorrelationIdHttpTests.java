package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import ca.bc.gov.open.digitalformsapi.viirp.filter.CorrelationIdFilter;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.JsonNode;

class CorrelationIdHttpTests extends HttpApiTestSupport {

    private static final String HEADER = CorrelationIdFilter.HEADER;
    private static final String PING = "/utility/ords/ping";
    private static final String PING_LOG = "Heard a call to 'pingOrds'";

    private final Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        rootLogger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        rootLogger.detachAppender(logs);
        logs.stop();
    }

    @Test
    void omittedHeaderIsReplacedByAGeneratedUuidThatIsEchoedAndLogged() throws Exception {
        String generated = generatedId(ping(List.of()));
        assertLogged(PING_LOG, generated);
    }

    static Stream<String> validIds() {
        return Stream.of("a", "abc.DEF_123-xyz", UUID.randomUUID().toString(), "x".repeat(64));
    }

    @ParameterizedTest
    @MethodSource("validIds")
    void validHeaderIsEchoedAndLogged(String id) throws Exception {
        assertEquals(List.of(id), ping(List.of(id)).headers().allValues(HEADER));
        assertLogged(PING_LOG, id);
    }

    static Stream<List<String>> invalidHeaders() {
        return Stream.of(List.of(""), List.of("x".repeat(65)), List.of("abc def"), List.of("a,b"),
                List.of("a/b"), List.of("<script>"), List.of("first", "second"));
    }

    @ParameterizedTest
    @MethodSource("invalidHeaders")
    void invalidOrRepeatedHeaderIsRejectedBeforeAuthenticationWithoutDownstreamCalls(List<String> ids)
            throws Exception {
        for (String auth : new String[] {null, AUTH}) {
            var response = requestWithCorrelationIds("POST", "/prohibitions", ProhibitionsHttpTests.BODY, auth,
                    "application/json", "application/json", ids);
            JsonNode error = responseJson(response, 400);
            assertEquals(CorrelationIdFilter.INVALID_MESSAGE, error.path("status_message").asString());
            String generated = generatedId(response);
            assertFalse(ids.contains(generated));
            assertLogged("Request rejected (400): invalid " + HEADER + " header", generated);
        }
        // No downstream response was queued, so the base class fails the test if the request reached VIPS.
    }

    @Test
    void contractDeclaresTheHeaderOnEveryOperationAndResponseWithTheFilterPattern() {
        JsonNode components = CONTRACT_JSON.path("components");
        JsonNode parameter = components.path("parameters").path("CorrelationId");
        assertEquals(HEADER, parameter.path("name").asString());
        assertEquals("header", parameter.path("in").asString());
        assertFalse(parameter.path("required").booleanValue());
        assertEquals(CorrelationIdFilter.PATTERN, parameter.path("schema").path("pattern").asString());
        JsonNode header = components.path("headers").path(HEADER);
        assertTrue(header.path("required").booleanValue());
        assertEquals(CorrelationIdFilter.PATTERN, header.path("schema").path("pattern").asString());

        int operations = 0;
        for (var path : CONTRACT_JSON.path("paths").properties()) {
            for (var operation : path.getValue().properties()) {
                String name = operation.getKey().toUpperCase(Locale.ROOT) + " " + path.getKey();
                operations++;
                assertTrue(operation.getValue().path("parameters").toString()
                        .contains("\"#/components/parameters/CorrelationId\""), name + " must accept the header");
                for (var response : operation.getValue().path("responses").properties()) {
                    JsonNode resolved = response.getValue();
                    if (resolved.has("$ref")) {
                        resolved = components.path("responses")
                                .path(resolved.path("$ref").asString().replace("#/components/responses/", ""));
                    }
                    assertEquals("#/components/headers/" + HEADER,
                            resolved.path("headers").path(HEADER).path("$ref").asString(),
                            name + " " + response.getKey() + " must return the header");
                }
            }
        }
        assertEquals(10, operations);
    }

    private HttpResponse<String> ping(List<String> ids) throws Exception {
        vips.enqueue(200, "{\"status\":\"UP\"}");
        var response = requestWithCorrelationIds("GET", PING, null, AUTH, null, "application/json", ids);
        responseJson(response, 200);
        assertNotNull(vips.requests.poll(2, TimeUnit.SECONDS), "Missing downstream ping");
        return response;
    }

    private static String generatedId(HttpResponse<String> response) {
        List<String> values = response.headers().allValues(HEADER);
        assertEquals(1, values.size(), "Exactly one " + HEADER + " response header expected");
        String id = values.get(0);
        assertEquals(id, UUID.fromString(id).toString(), "Generated correlation ID must be a UUID");
        return id;
    }

    private void assertLogged(String message, String correlationId) {
        assertTrue(logs.list.stream().anyMatch(event -> event.getFormattedMessage().equals(message)
                && correlationId.equals(event.getMDCPropertyMap().get(DigitalFormsConstants.REQUEST_CORRELATION_ID))),
                "Expected '" + message + "' to be logged with correlation ID " + correlationId);
    }
}
