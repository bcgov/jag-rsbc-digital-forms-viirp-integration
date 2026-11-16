package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static ca.bc.gov.open.digitalformsapi.viirp.controller.TestIdentityProvider.accessTokenClaims;
import static ca.bc.gov.open.digitalformsapi.viirp.controller.TestIdentityProvider.bearer;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** {@code DIGITALFORMS_OAUTH2_AUDIENCE} additionally requires the API's audience in the token's aud claim. */
@TestPropertySource(properties = "DIGITALFORMS_OAUTH2_AUDIENCE=" + TestIdentityProvider.AUDIENCE)
class OAuth2AudienceHttpTests extends HttpApiTestSupport {

    @Test
    void tokensMustNameTheConfiguredAudience() throws Exception {
        responseJson(request("GET", "/actuator/info", null, AUTH), 200);

        var response = request("GET", "/codetables", null, bearer(accessTokenClaims().audience("account")));
        responseJson(response, 401);
        String challenge = response.headers().firstValue("WWW-Authenticate").orElse("");
        assertTrue(challenge.contains("error=\"invalid_token\""), challenge);
    }
}
