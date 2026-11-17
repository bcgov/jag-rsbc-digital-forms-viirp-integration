package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static ca.bc.gov.open.digitalformsapi.viirp.controller.TestIdentityProvider.ISSUER;
import static ca.bc.gov.open.digitalformsapi.viirp.controller.TestIdentityProvider.KEY_ID;
import static ca.bc.gov.open.digitalformsapi.viirp.controller.TestIdentityProvider.accessTokenClaims;
import static ca.bc.gov.open.digitalformsapi.viirp.controller.TestIdentityProvider.bearer;
import static ca.bc.gov.open.digitalformsapi.viirp.controller.TestIdentityProvider.newKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Stream;

import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.FilterChainProxy;

import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsAuthenticationFailureHandler;

class OAuth2HttpTests extends HttpApiTestSupport {

    private static final String CHALLENGE = "Bearer realm=\"" + DigitalFormsAuthenticationFailureHandler.REALM + "\"";

    @Autowired
    private FilterChainProxy securityFilterChain;

    @Autowired
    private DigitalFormsAuthenticationFailureHandler failureHandler;

    @Test
    void csrfRejectsNonBearerWritesBeforeTheApplication() throws Exception {
        var request = new MockHttpServletRequest("POST", "/prohibitions");
        request.setServletPath("/prohibitions");
        var response = new MockHttpServletResponse();
        securityFilterChain.doFilter(request, response,
                (req, res) -> fail("An unauthenticated write must not reach the application"));
        // Inspect the filter response before Tomcat's /error dispatch, which requires authentication.
        assertEquals(403, response.getStatus());
    }

    static Stream<Arguments> rejectedTokens() {
        Instant now = Instant.now();
        return Stream.of(
                Arguments.of("expired", bearer(accessTokenClaims()
                        .issueTime(Date.from(now.minus(10, ChronoUnit.MINUTES)))
                        .expirationTime(Date.from(now.minus(2, ChronoUnit.MINUTES))))),
                Arguments.of("another realm", bearer(accessTokenClaims().issuer(ISSUER.replace("/Services", "/Other")))),
                Arguments.of("unlisted client", UNLISTED_CLIENT_AUTH),
                Arguments.of("no authorized party", bearer(accessTokenClaims().claim("azp", null))),
                Arguments.of("ID token", bearer(accessTokenClaims().claim("typ", "ID"))),
                Arguments.of("no token type", bearer(accessTokenClaims().claim("typ", null))),
                Arguments.of("forged signature", bearer(accessTokenClaims(), newKey(KEY_ID))),
                Arguments.of("unknown signing key", bearer(accessTokenClaims(), newKey("unknown-key"))),
                Arguments.of("unsigned", "Bearer " + new PlainJWT(accessTokenClaims().build()).serialize()),
                Arguments.of("not a JWT", "Bearer not-a-jwt"),
                Arguments.of("malformed header", "Bearer not a token"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rejectedTokens")
    void rejectedTokensGetAnInvalidTokenChallengeWithoutReachingVips(String name, String authorization)
            throws Exception {
        var response = request("GET", "/codetables", null, authorization);
        assertEquals(DigitalFormsAuthenticationFailureHandler.UNAUTHORIZED_MESSAGE,
                responseJson(response, 401).path("detail").asString());
        String challenge = response.headers().firstValue("WWW-Authenticate").orElse("");
        assertTrue(challenge.startsWith(CHALLENGE), challenge);
        assertTrue(challenge.contains("error=\"invalid_token\""), challenge);
    }

    @Test
    void requestsWithoutABearerTokenGetAChallengeThatLocatesTheMetadata() throws Exception {
        for (String authorization : new String[] {null, basicAuth(TestIdentityProvider.CLIENT_ID, "secret")}) {
            for (String method : new String[] {"GET", "POST"}) {
                var response = request(method, "/prohibitions", null, authorization);
                responseJson(response, 401);
                String challenge = response.headers().firstValue("WWW-Authenticate").orElse("");
                assertTrue(challenge.startsWith(CHALLENGE), challenge);
                assertFalse(challenge.contains("error="), challenge);
                assertTrue(challenge.contains(
                        "resource_metadata=\"" + apiBaseUrl() + "/.well-known/oauth-protected-resource\""), challenge);
            }
        }
    }

    @Test
    void malformedBearerRequestPreserves400AndChallengeWithProblemDetails() throws Exception {
        var request = new MockHttpServletRequest("GET", "/digitalforms-viirp/v1/codetables");
        var response = new MockHttpServletResponse();
        failureHandler.commence(request, response,
                new OAuth2AuthenticationException(BearerTokenErrors.invalidRequest("Invalid bearer request")));
        assertEquals(400, response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
        var problem = json.readTree(response.getContentAsString());
        assertEquals(400, problem.path("status").asInt());
        assertEquals("Bad Request", problem.path("detail").asString());
        assertEquals(request.getRequestURI(), problem.path("instance").asString());
        assertTrue(response.getHeader("WWW-Authenticate").contains("error=\"invalid_request\""));
    }

    @Test
    void bearerAccessDeniedPreserves403AndScopeChallengeWithSafeProblemDetails() throws Exception {
        var request = new MockHttpServletRequest("GET", "/digitalforms-viirp/v1/codetables");
        request.setUserPrincipal(new JwtAuthenticationToken(Jwt.withTokenValue("not-for-response")
                .header("alg", "RS256").subject("test-client").build()));
        var response = new MockHttpServletResponse();
        failureHandler.handle(request, response, new AccessDeniedException("hidden-access-decision"));
        assertEquals(403, response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
        var problem = json.readTree(response.getContentAsString());
        assertEquals(403, problem.path("status").asInt());
        assertEquals("Forbidden", problem.path("detail").asString());
        assertEquals(request.getRequestURI(), problem.path("instance").asString());
        assertTrue(response.getHeader("WWW-Authenticate").contains("error=\"insufficient_scope\""));
        assertFalse(response.getContentAsString().contains("not-for-response"));
        assertFalse(response.getContentAsString().contains("hidden-access-decision"));
    }

    @Test
    void protectedResourceMetadataNamesDiamAsTheAuthorizationServer() throws Exception {
        var metadata = responseJson(request("GET", "/.well-known/oauth-protected-resource", null, null), 200);
        assertEquals(apiBaseUrl(), metadata.path("resource").asString());
        assertEquals(json.readTree("[\"" + ISSUER + "\"]"), metadata.path("authorization_servers"));
        assertEquals(json.readTree("[\"header\"]"), metadata.path("bearer_methods_supported"));
        assertFalse(metadata.path("tls_client_certificate_bound_access_tokens").asBoolean(true));
    }
}
