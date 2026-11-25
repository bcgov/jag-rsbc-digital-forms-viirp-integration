package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;

/**
 * The PROD switch: {@code SPRINGDOC_APIDOCS_ENABLED=false} turns off the API docs and Swagger UI,
 * even when {@code DIGITALFORMS_SWAGGER_ENABLED=true}, without affecting the API itself.
 */
@TestPropertySource(properties = {"SPRINGDOC_APIDOCS_ENABLED=false", "DIGITALFORMS_SWAGGER_ENABLED=true"})
class ApiDocsDisabledHttpTests extends HttpApiTestSupport {

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/swagger-config",
            "/swagger-ui/index.html", "/swagger-ui.html"})
    void apiDocsAndSwaggerUiAreNotPublished(String path) throws Exception {
        assertEquals(404, request("GET", path, null, null, null, "*/*").statusCode());
        assertEquals(404, request("GET", path, null, AUTH, null, "*/*").statusCode());
    }

    @Test
    void apiRemainsAvailableAndSecured() throws Exception {
        assertEquals("UP", responseJson(request("GET", "/actuator/health/liveness", null, null), 200).path("status").asString());
        assertEquals("UP", responseJson(request("GET", "/actuator/health/readiness", null, null), 200).path("status").asString());
        vips.enqueue(200, "{\"status\":\"success\"}");
        assertEquals("UP", responseJson(request("GET", "/actuator/health/ords", null, null), 200).path("status").asString());
        assertNotNull(vips.requests.poll(2, TimeUnit.SECONDS));
        responseJson(request("GET", "/codetables", null, null), 401);
        responseJson(request("GET", "/actuator/info", null, null), 401);
        responseJson(request("GET", "/actuator/info", null, AUTH), 200);
    }
}
