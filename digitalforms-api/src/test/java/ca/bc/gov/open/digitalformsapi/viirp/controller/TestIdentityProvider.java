package ca.bc.gov.open.digitalformsapi.viirp.controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Stands in for DIAM: mints RS256 access tokens shaped like DIAM client-credentials tokens and serves the public key
 * set, so the API's real JwtDecoder validates them without network access.
 */
final class TestIdentityProvider implements AutoCloseable {

    /** The application.yml default, so the runtime OpenAPI token URL matches the contract. */
    static final String ISSUER = "https://common-sso.justice.gov.bc.ca/auth/realms/Services";
    static final String CLIENT_ID = "route-test-client";
    static final String AUDIENCE = "route-test-api";
    static final String KEY_ID = "test-key";
    private static final String JWKS_PATH = "/protocol/openid-connect/certs";
    private static final RSAKey SIGNING_KEY = newKey(KEY_ID);

    final AtomicInteger jwksRequests = new AtomicInteger();
    private final int jwksStatus;
    private final HttpServer server;

    TestIdentityProvider(int jwksStatus) throws IOException {
        this.jwksStatus = jwksStatus;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(JWKS_PATH, this::serveKeys);
        server.start();
    }

    String jwkSetUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + JWKS_PATH;
    }

    /** The claims of a valid access token for {@link #CLIENT_ID}; adjust them to build rejected variants. */
    static JWTClaimsSet.Builder accessTokenClaims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE, "account"))
                .subject(UUID.randomUUID().toString())
                .claim("azp", CLIENT_ID)
                .claim("client_id", CLIENT_ID)
                .claim("typ", "Bearer")
                .claim("scope", "email profile")
                .claim("preferred_username", "service-account-" + CLIENT_ID)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .jwtID(UUID.randomUUID().toString());
    }

    static String bearer(JWTClaimsSet.Builder claims) {
        return bearer(claims, SIGNING_KEY);
    }

    static String bearer(JWTClaimsSet.Builder claims, RSAKey key) {
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID())
                .type(JOSEObjectType.JWT).build(), claims.build());
        try {
            jwt.sign(new RSASSASigner(key));
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        return "Bearer " + jwt.serialize();
    }

    static RSAKey newKey(String keyId) {
        try {
            return new RSAKeyGenerator(2048).keyID(keyId).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private void serveKeys(HttpExchange exchange) throws IOException {
        try (exchange) {
            jwksRequests.incrementAndGet();
            String body = jwksStatus == 200 ? new JWKSet(SIGNING_KEY).toPublicJWKSet().toString()
                    : "{\"error\":\"unavailable\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(jwksStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
