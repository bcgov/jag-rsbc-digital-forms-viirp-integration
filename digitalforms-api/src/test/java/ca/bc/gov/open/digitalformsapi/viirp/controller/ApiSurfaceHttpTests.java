package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
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
        assertEquals(CONTRACT_JSON.path("openapi"), api.path("openapi"),
                "Runtime docs must use the contract's OpenAPI version; 3.1 deprecates the models' schema examples");
        assertEquals(CONTRACT_JSON.path("info"), api.path("info"), "API metadata must come from the contract");
        assertEquals(apiBaseUrl(), api.path("servers").path(0).path("url").asString(),
                "The server URL must be absolute so that clients such as Postman resolve it correctly");
        assertEquals(10, CONTRACT_JSON.path("paths").size(),
                "The canonical YAML remains the business API contract and excludes Actuator");
        JsonNode paths = api.path("paths");
        assertEquals(13, paths.size(),
                "Runtime docs should include the exposed Actuator endpoints and authenticated discovery: "
                        + paths.propertyNames());
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
        assertTrue(paths.path("/actuator/health").has("get"));
        assertTrue(paths.path("/actuator/info").has("get"));
        assertTrue(paths.path("/actuator").has("get"));
        assertTrue(paths.path("/actuator/health").path("get").path("security").isMissingNode()
                || paths.path("/actuator/health").path("get").path("security").isEmpty(),
                "Public health must not advertise authentication");
        for (String path : List.of("/actuator", "/actuator/info")) {
            var security = paths.path(path).path("get").path("security");
            assertEquals(1, security.size(), path + " must advertise authentication");
            assertTrue(security.get(0).has("basicAuth"), path + " must use the existing Basic scheme");
        }
        assertFalse(paths.has("/actuator/health/{*path}"), "Health component subpaths are not exposed");
        assertFalse(api.toString().contains("{correlationId}"), "Correlation ID is a header, not a path variable");
        assertFalse(api.toString().toLowerCase(Locale.ROOT).contains("dfpayload"));
        responseJson(request("GET", "/unknown", null, AUTH), 404);
    }

    @Test
    void healthOnlyReturnsStatusAndOptionalProbeGroupNamesEvenWhenAuthenticated() throws Exception {
        for (String authorization : new String[] {null, AUTH}) {
            var health = responseJson(request("GET", "/actuator/health", null, authorization), 200);
            assertEquals("UP", health.path("status").asString());
            assertTrue(Set.of("status", "groups").containsAll(health.propertyNames()), health.toString());
        }
        responseJson(request("GET", "/actuator/health/diskSpace", null, null), 401);
    }

    @Test
    void actuatorInformationAndDiscoveryRequireAuthentication() throws Exception {
        for (String path : List.of("/actuator/info", "/actuator")) {
            for (String authorization : new String[] {null, basicAuth("route-test", "wrong-password")}) {
                var response = request("GET", path, null, authorization);
                responseJson(response, 401);
                assertTrue(response.headers().firstValue("WWW-Authenticate").orElse("").startsWith("Basic "));
                assertFalse(response.body().contains("\"build\""));
                assertFalse(response.body().contains("\"java\""));
            }
        }

        var properties = new Properties();
        try (var input = new ClassPathResource("META-INF/build-info.properties").getInputStream()) {
            properties.load(input);
        }
        var info = responseJson(request("GET", "/actuator/info", null, AUTH), 200);
        assertEquals(Set.of("build", "java"), info.propertyNames());
        assertEquals(properties.getProperty("build.version"), info.at("/build/version").asString());
        assertEquals(properties.getProperty("build.artifact"), info.at("/build/artifact").asString());
        assertEquals(System.getProperty("java.version"), info.at("/java/version").asString());
        assertEquals(System.getProperty("java.runtime.version"), info.at("/java/runtime/version").asString());
        assertEquals(System.getProperty("java.vm.version"), info.at("/java/jvm/version").asString());
    }

    @Test
    void onlyHealthAndInfoAreExposedAsActuatorEndpoints() throws Exception {
        var links = responseJson(request("GET", "/actuator", null, AUTH), 200).path("_links");
        assertEquals(Set.of("self", "health", "health-path", "info"), links.propertyNames());
        for (String path : List.of("/actuator/env", "/actuator/beans", "/actuator/metrics",
                "/actuator/configprops", "/actuator/heapdump")) {
            responseJson(request("GET", path, null, null), 401);
            responseJson(request("GET", path, null, AUTH), 404);
        }
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
    void publishedUploadIsMultipartWithOneBinaryFileAndUnchangedResponse() throws Exception {
        var api = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        var operation = api.path("paths").path("/documents").path("post");
        var content = operation.path("requestBody").path("content");
        assertEquals(1, content.size());
        var schema = content.path("multipart/form-data").path("schema");
        assertEquals("object", schema.path("type").asString());
        assertEquals("string", schema.path("properties").path("file").path("type").asString());
        assertEquals("binary", schema.path("properties").path("file").path("format").asString());
        assertTrue(schema.path("required").toString().contains("\"file\""));
        assertTrue(schema.path("required").toString().contains("\"type_code\""), schema.toPrettyString());
        assertFalse(schema.path("properties").has("file_object"));
        assertFalse(schema.path("properties").has("mime_type"));
        assertFalse(schema.path("properties").has("mime_sub_type"));
        assertFalse(schema.path("properties").has("pageCount"));
        assertTrue(operation.path("responses").has("413"));
        var response = operation.path("responses").path("201").path("content")
                .path("application/json").path("schema");
        assertEquals("#/components/schemas/VipsDocumentResponse", response.path("$ref").asString());
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
