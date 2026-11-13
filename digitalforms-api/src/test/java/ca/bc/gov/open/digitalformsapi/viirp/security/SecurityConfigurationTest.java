package ca.bc.gov.open.digitalformsapi.viirp.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigurationTest {

    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @Test
    void allowedClientIdsAreATrimmedCommaSeparatedList() {
        var validator = configuration.authorizedClientValidator(" vips-vi-irp-dev , other-client ,");
        assertFalse(validator.validate(token("vips-vi-irp-dev")).hasErrors());
        assertFalse(validator.validate(token("other-client")).hasErrors());
        assertTrue(validator.validate(token("vips-vi-irp")).hasErrors());
        assertTrue(validator.validate(token(null)).hasErrors());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", " , "})
    void startupFailsWithoutAnAllowedClient(String allowedClientIds) {
        assertThrows(IllegalStateException.class, () -> configuration.authorizedClientValidator(allowedClientIds));
    }

    private static Jwt token(String authorizedParty) {
        var token = Jwt.withTokenValue("token").header("alg", "RS256").subject("service-account");
        if (authorizedParty != null) {
            token.claim(SecurityConfiguration.AUTHORIZED_PARTY_CLAIM, authorizedParty);
        }
        return token.build();
    }
}
