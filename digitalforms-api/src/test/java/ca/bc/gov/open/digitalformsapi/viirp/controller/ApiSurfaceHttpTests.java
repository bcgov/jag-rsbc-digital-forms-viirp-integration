package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import ca.bc.gov.open.digitalformsapi.viirp.filter.CorrelationIdFilter;
import tools.jackson.databind.JsonNode;

@TestPropertySource(properties = "springdoc.swagger-ui.enabled=true")
class ApiSurfaceHttpTests extends HttpApiTestSupport {

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE"})
    void removedDfPayloadOperationsReturn404WithoutDownstreamCalls(String method) throws Exception {
        String path = "/dfpayloads/21900601";
        String body = method.equals("POST") || method.equals("PUT") ? "{}" : null;
        responseJson(request(method, path, body, null), 401);
        JsonNode error = responseJson(request(method, path, body, AUTH), 404);
        assertEquals("Not Found", error.path("status_message").asString());
    }

    @Test
    void publicDiscoveryRetainsSupportedRoutesAndExcludesDfPayloads() throws Exception {
        assertEquals("UP", responseJson(request("GET", "/actuator/health", null, null), 200).path("status").asString());
        JsonNode api = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        assertEquals(CONTRACT_JSON.path("info"), api.path("info"), "API metadata must come from the contract");
        assertEquals(apiBaseUrl(), api.path("servers").path(0).path("url").asString(),
                "The server URL must be absolute so that clients such as Postman resolve it correctly");
        JsonNode paths = api.path("paths");
        assertEquals(10, paths.size());
        for (String route : new String[] {"prohibitions", "impoundments"}) {
            assertTrue(paths.path("/" + route).has("post"));
            assertTrue(paths.path("/" + route + "/{noticeNo}").has("get"));
        }
        assertTrue(paths.path("/documents").has("post"));
        assertTrue(paths.path("/documents/{documentId}").has("get"));
        assertTrue(paths.path("/documents/list/{noticeNo}").has("get"));
        assertTrue(paths.path("/documents/association/notice/{documentId}").has("post"));
        assertTrue(paths.path("/codetables").has("get"));
        assertTrue(paths.path("/utility/ords/ping").has("get"));
        assertFalse(api.toString().contains("{correlationId}"), "Correlation ID is a header, not a path variable");
        assertFalse(api.toString().toLowerCase(Locale.ROOT).contains("dfpayload"));
        responseJson(request("GET", "/unknown", null, AUTH), 404);
    }

    @Test
    void swaggerUiLoadsThePublishedApiDocs() throws Exception {
        var yamlResponse = request("GET", "/v3/api-docs.yaml", null, null, null, "*/*");
        assertEquals(200, yamlResponse.statusCode());
        JsonNode yamlDocs = json.valueToTree(new Yaml(new SafeConstructor(new LoaderOptions())).load(yamlResponse.body()));
        assertEquals(CONTRACT_JSON.path("info"), yamlDocs.path("info"));
        var config = responseJson(request("GET", "/v3/api-docs/swagger-config", null, null), 200);
        assertEquals("/digitalforms-viirp/v1/v3/api-docs", config.path("url").asString());
        var ui = request("GET", "/swagger-ui/index.html", null, null, null, "text/html");
        assertEquals(200, ui.statusCode());
        assertTrue(ui.body().contains("swagger-ui"));
    }

    @Test
    void contractGuardRejectsWrongStatusSchemaContentTypeAndCorrelationHeader() {
        String path = "/prohibitions";
        String ok = "{\"respMsg\":\"success\",\"prohibitionId\":123456789012345}";
        assertConforms("POST", path, 201, ok, headers("application/json", correlationId));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 200, ok, headers("application/json", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, "{\"respMsg\":[]}", headers("application/json", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, "{\"respMsg\":\"success\"}", headers("application/json", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, "{}", headers("application/json", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 500, "{}", headers("application/json", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, ok, headers("text/plain", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, ok, headers("application/json", null)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, ok, headers("application/json", "not valid")));
    }

    private static Map<String, List<String>> headers(String contentType, String correlationId) {
        return correlationId == null
                ? Map.of("Content-Type", List.of(contentType))
                : Map.of("Content-Type", List.of(contentType), CorrelationIdFilter.HEADER, List.of(correlationId));
    }
}
