package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Swagger UI stays off unless {@code DIGITALFORMS_SWAGGER_ENABLED=true}, while the API docs are published.
 * Assumes the test JVM does not set {@code DIGITALFORMS_SWAGGER_ENABLED}.
 */
class SwaggerUiDefaultHttpTests extends HttpApiTestSupport {

    @ParameterizedTest
    @ValueSource(strings = {"/swagger-ui/index.html", "/swagger-ui.html", "/v3/api-docs/swagger-config"})
    void swaggerUiIsDisabledByDefault(String path) throws Exception {
        assertEquals(404, request("GET", path, null, null, null, "*/*").statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs.yaml"})
    void apiDocsArePublishedByDefault(String path) throws Exception {
        assertEquals(200, request("GET", path, null, null, null, "*/*").statusCode());
    }
}
