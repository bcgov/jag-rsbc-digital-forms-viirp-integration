package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RequestErrorsHttpTests extends HttpApiTestSupport {

    @Test
    void preferringProblemDetailsDoesNotMislabelSuccessfulJson() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"configuration\":{\"impoundLotOperatorDetails\":[]}}");
        responseJson(request("GET", "/codetables", null, AUTH, null,
                "application/problem+json, application/json;q=0.5"), 200);
        assertNotNull(vips.requests.poll(2, TimeUnit.SECONDS));
    }

    @Test
    void validationFailuresUseProblemDetailsWithSeparateFieldErrors() throws Exception {
        var response = request("POST", "/prohibitions", "{}", AUTH);
        assertEquals(400, response.statusCode());
        assertEquals("application/problem+json", response.headers().firstValue("Content-Type").orElseThrow());
        var problem = json.readTree(response.body());
        assertEquals("about:blank", problem.path("type").asString());
        assertEquals("Bad Request", problem.path("title").asString());
        assertEquals(400, problem.path("status").asInt());
        assertEquals("Request validation failed.", problem.path("detail").asString());
        assertEquals(response.uri().getRawPath(), problem.path("instance").asString());
        assertFalse(problem.has("status_message"));
        Set<String> fields = new HashSet<>();
        for (var error : problem.path("errors")) {
            assertEquals(Set.of("field", "message"), error.propertyNames());
            assertFalse(error.path("message").asString().isBlank());
            fields.add(error.path("field").asString());
        }
        assertEquals(Set.of("prohibition", "registration"), fields);
    }

    @Test
    void upstreamFailuresUseSafeProblemDetailsWithoutQueryParameters() throws Exception {
        vips.enqueue(500, "{\"error\":\"ORA-20011: hidden-database-details\"}");
        var response = request("GET", "/codetables?diagnostic=not-for-response", null, AUTH);
        assertEquals(502, response.statusCode());
        assertEquals("application/problem+json", response.headers().firstValue("Content-Type").orElseThrow());
        var problem = json.readTree(response.body());
        assertEquals(Set.of("type", "title", "status", "detail", "instance"), problem.propertyNames());
        assertEquals("Bad Gateway", problem.path("title").asString());
        assertEquals("Invalid response from VIPS.", problem.path("detail").asString());
        assertEquals(502, problem.path("status").asInt());
        assertEquals(response.uri().getRawPath(), problem.path("instance").asString());
        assertFalse(response.body().contains("hidden-database-details"));
        assertFalse(response.body().contains("not-for-response"));
        var upstream = vips.requests.poll(2, TimeUnit.SECONDS);
        assertNotNull(upstream);
        assertEquals("/configuration", upstream.uri().getPath());
    }

    @ParameterizedTest
    @ValueSource(strings = {"prohibitions", "impoundments"})
    void missingAndMalformedBodiesReturnSafe400(String route) throws Exception {
        String path = "/" + route;
        for (String body : new String[] {null, "", "null", "{", "{}", "[]"}) {
            var error = responseJson(request("POST", path, body, AUTH, "application/json", "application/json"), 400);
            assertFalse(error.path("errors").isEmpty());
            assertFalse(error.toString().contains("org.springframework"));
            assertFalse(error.toString().contains("public "));
            assertFalse(error.toString().contains("java."));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "9223372036854775808"})
    void invalidDocumentIdsReturn400(String id) throws Exception {
        var error = responseJson(request("GET", "/documents/" + id, null, AUTH, null,
                "application/pdf, application/problem+json"), 400);
        assertEquals("documentId", error.at("/errors/0/field").asString(), error.toString());
    }

    @Test
    void unsupportedMethodPreservesAllowHeader() throws Exception {
        var response = request("PUT", "/prohibitions", "{}", AUTH);
        responseJson(response, 405);
        assertTrue(response.headers().firstValue("Allow").orElse("").contains("POST"));
    }

    @Test
    void mediaTypeFailuresAreNotInternalErrors() throws Exception {
        responseJson(request("POST", "/prohibitions", "{}", AUTH,
                "text/plain", "application/json"), 415);
        responseJson(request("GET", "/codetables", null, AUTH, null, "application/xml"), 406);
        responseJson(request("GET", "/codetables", null, AUTH, null, "application/problem+json"), 406);
    }

    @Test
    void unauthorizedResponseIncludesBearerChallenge() throws Exception {
        var response = request("GET", "/codetables", null, null);
        var body = responseJson(response, 401);
        assertEquals("401 - Unauthorized entry, please authenticate", body.path("detail").asString());
        assertTrue(response.headers().firstValue("WWW-Authenticate").orElse("")
                .startsWith("Bearer realm=\"DigitalForms - VI IRP API\""));
    }

    @Test
    void requiredNoticeAndNonemptyImpoundmentCollectionsAreValidated() throws Exception {
        responseJson(request("POST", "/prohibitions",
                ProhibitionsHttpTests.BODY.replace("\"prohibitionNoticeNo\": \"21900601\",", ""), AUTH), 400);
        responseJson(request("POST", "/prohibitions",
                ProhibitionsHttpTests.BODY.replace("\"21900601\"", "\"   \""), AUTH), 400);
        var impoundment = json.readTree(ImpoundmentsHttpTests.BODY);
        impoundment.withArray("registrations").removeAll();
        responseJson(request("POST", "/impoundments", impoundment.toString(), AUTH), 400);
        impoundment.withArray("registrations").addNull();
        responseJson(request("POST", "/impoundments", impoundment.toString(), AUTH), 400);
    }
}
