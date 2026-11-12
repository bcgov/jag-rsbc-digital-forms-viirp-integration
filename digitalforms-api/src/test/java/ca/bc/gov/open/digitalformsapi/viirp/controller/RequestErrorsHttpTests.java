package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RequestErrorsHttpTests extends HttpApiTestSupport {

    @ParameterizedTest
    @ValueSource(strings = {"prohibitions", "impoundments", "documents", "documents/association/notice/123"})
    void missingAndMalformedBodiesReturnSafe400(String route) throws Exception {
        String path = "/" + route;
        for (String body : new String[] {null, "", "null", "{", "{}", "[]"}) {
            var error = responseJson(request("POST", path, body, AUTH, "application/json", "application/json"), 400);
            assertTrue(error.hasNonNull("status_message"));
            assertFalse(error.toString().contains("org.springframework"));
            assertFalse(error.toString().contains("public "));
            assertFalse(error.toString().contains("java."));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "9223372036854775808"})
    void invalidDocumentIdsReturn400(String id) throws Exception {
        responseJson(request("GET", "/documents/" + id, null, AUTH), 400);
        responseJson(request("POST", "/documents/association/notice/" + id,
                "{\"noticeNo\":\"81000029\",\"noticeTypeCd\":\"IRP\"}", AUTH), 400);
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
        assertEquals(406, request("GET", "/codetables", null, AUTH,
                null, "application/xml").statusCode());
    }

    @Test
    void unauthorizedResponseIncludesBasicChallenge() throws Exception {
        var response = request("GET", "/codetables", null, null);
        var body = responseJson(response, 401);
        assertEquals("401 - Unauthorized entry, please authenticate", body.path("status_message").asString());
        assertTrue(response.headers().firstValue("WWW-Authenticate").orElse("").startsWith("Basic "));
    }

    @Test
    void requiredNoticeAndNonemptyImpoundmentCollectionsAreValidated() throws Exception {
        responseJson(request("POST", "/prohibitions",
                ProhibitionsHttpTests.BODY.replace("\"prohibitionNoticeNo\": \"21900601\",", ""), AUTH), 400);
        responseJson(request("POST", "/prohibitions",
                ProhibitionsHttpTests.BODY.replace("\"21900601\"", "\"   \""), AUTH), 400);
        var impoundment = json.readTree(ImpoundmentsHttpTests.BODY);
        impoundment.withArray("vipsRegistrationCreateArray").removeAll();
        responseJson(request("POST", "/impoundments", impoundment.toString(), AUTH), 400);
        impoundment.withArray("vipsRegistrationCreateArray").addNull();
        responseJson(request("POST", "/impoundments", impoundment.toString(), AUTH), 400);
    }
}
