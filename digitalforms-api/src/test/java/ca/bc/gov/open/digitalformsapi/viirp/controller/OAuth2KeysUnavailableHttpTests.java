package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** DIAM's signing keys cannot be downloaded: presented tokens cannot be checked, which is a server-side failure. */
@TestPropertySource(properties = "test.oauth2.jwks-status=503")
class OAuth2KeysUnavailableHttpTests extends HttpApiTestSupport {

    @Test
    void unverifiableTokensReturn500WithoutReachingVips() throws Exception {
        var error = responseJson(request("GET", "/codetables", null, AUTH), 500);
        assertEquals("Internal Server Error", error.path("status_message").asString());
        assertTrue(identityProvider.jwksRequests.get() > 0, "The API must try to download DIAM's keys");

        responseJson(request("GET", "/codetables", null, null), 401);
        assertEquals("UP", responseJson(request("GET", "/actuator/health", null, null), 200).path("status").asString());
    }
}
