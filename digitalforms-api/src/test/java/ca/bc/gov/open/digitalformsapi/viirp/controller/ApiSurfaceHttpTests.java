package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

class ApiSurfaceHttpTests extends HttpApiTestSupport {

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE"})
    void removedDfPayloadOperationsReturn404WithoutDownstreamCalls(String method) throws Exception {
        String path = "/dfpayloads/21900601/" + correlationId;
        String body = method.equals("POST") || method.equals("PUT") ? "{}" : null;
        responseJson(request(method, path, body, null), 401);
        JsonNode error = responseJson(request(method, path, body, AUTH), 404);
        assertTrue(error.path("status_message").asText().contains("dfpayloads"));
    }

    @Test
    void publicDiscoveryRetainsSupportedRoutesAndExcludesDfPayloads() throws Exception {
        assertEquals("UP", responseJson(request("GET", "/actuator/health", null, null), 200).path("status").asText());
        JsonNode api = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        JsonNode paths = api.path("paths");
        assertEquals(10, paths.size());
        for (String route : new String[] {"prohibitions", "impoundments"}) {
            assertTrue(paths.path("/" + route + "/{correlationId}").has("post"));
            assertTrue(paths.path("/" + route + "/{noticeNo}/{correlationId}").has("get"));
        }
        assertTrue(paths.path("/documents/{correlationId}").has("post"));
        assertTrue(paths.path("/documents/{documentId}/{correlationId}").has("get"));
        assertTrue(paths.path("/documents/list/{noticeNo}/{correlationId}").has("get"));
        assertTrue(paths.path("/documents/association/notice/{documentId}/{correlationId}").has("post"));
        assertTrue(paths.path("/codetables/{correlationId}").has("get"));
        assertTrue(paths.path("/utility/ords/ping/{correlationId}").has("get"));
        assertFalse(api.toString().toLowerCase(Locale.ROOT).contains("dfpayload"));
        responseJson(request("GET", "/unknown/" + correlationId, null, AUTH), 404);
    }
}
