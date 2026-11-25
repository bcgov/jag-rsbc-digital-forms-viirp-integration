package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ca.bc.gov.open.digitalformsapi.viirp.exception.UpstreamServiceException;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsOrdsService;

class UpstreamFailuresHttpTests extends HttpApiTestSupport {

    @Autowired
    private VipsOrdsService ords;

    @ParameterizedTest
    @CsvSource({"400,502", "401,502", "404,502", "408,504", "429,503",
            "500,502", "502,502", "503,503", "504,504"})
    void distinguishesVipsHttpFailures(int upstreamStatus, int expectedStatus) throws Exception {
        vips.enqueue(upstreamStatus, "{\"error\":\"ORA-20011: hidden-database-details\"}");
        var response = request("GET", "/codetables", null, AUTH);
        var problem = responseJson(response, expectedStatus);
        assertEquals(Set.of("type", "title", "status", "detail", "instance"), problem.propertyNames());
        assertEquals(HttpStatus.valueOf(expectedStatus).getReasonPhrase(), problem.path("title").asString());
        assertFalse(response.body().contains("hidden-database-details"));
        assertFalse(response.body().contains(vips.baseUrl()));
        if (expectedStatus == 503) {
            assertEquals(List.of("30"), response.headers().allValues("Retry-After"));
        } else {
            assertTrue(response.headers().allValues("Retry-After").isEmpty());
        }
        var upstream = vips.requests.poll(2, TimeUnit.SECONDS);
        assertNotNull(upstream);
        assertEquals("/configuration", upstream.uri().getPath());
    }

    @ParameterizedTest
    @ValueSource(strings = {"120", "Wed, 18 Nov 2026 20:00:00 GMT", "-1", "not-an-http-date"})
    void retryAfterRetainsOnlyValidUpstreamValues(String value) throws Exception {
        vips.enqueue(503, "{\"error\":\"unavailable\"}", Map.of("Retry-After", value));
        var response = request("GET", "/codetables", null, AUTH);
        responseJson(response, 503);
        assertEquals(List.of(value.startsWith("-") || value.startsWith("not-") ? "30" : value),
                response.headers().allValues("Retry-After"));
        assertNotNull(vips.requests.poll(2, TimeUnit.SECONDS));
        vips.enqueue(503, "{\"error\":\"unavailable\"}", Map.of("Retry-After", value));
        var failure = assertThrows(UpstreamServiceException.class, ords::health);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failure.getStatus());
        assertEquals(value.startsWith("-") || value.startsWith("not-") ? "30" : value,
                failure.getRetryAfter());
        assertNotNull(vips.requests.poll(2, TimeUnit.SECONDS));
    }

    @Test
    void timedOutVipsCreateIs504AndIsNotResent() throws Exception {
        availableNotice();
        vips.responses.add(new StubResponse(200,
                "{\"respCd\":0,\"prohibitionId\":123}".getBytes(StandardCharsets.UTF_8),
                "application/json", Map.of(), 3000, 0));
        var response = request("POST", "/prohibitions", ProhibitionsHttpTests.BODY, AUTH);
        assertEquals("VIPS did not respond in time.", responseJson(response, 504).path("detail").asString());
        assertTrue(response.headers().allValues("Retry-After").isEmpty());
        checkedNotice("prohibition", "21900601");
        downstream("POST", "/cases/prohibitions");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{", "[]", "{}"})
    void malformedOrEmptyUpstreamJsonIs502(String body) throws Exception {
        vips.enqueue(200, body);
        var response = request("GET", "/codetables", null, AUTH);
        responseJson(response, 502);
        assertFalse(response.body().contains("Exception"));
        assertNotNull(vips.requests.poll(2, TimeUnit.SECONDS));
    }
}
