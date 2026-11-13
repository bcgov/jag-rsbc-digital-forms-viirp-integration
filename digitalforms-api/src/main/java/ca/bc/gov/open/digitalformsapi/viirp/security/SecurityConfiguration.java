package ca.bc.gov.open.digitalformsapi.viirp.security;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsAuthenticationFailureHandler;

/**
 * OAuth2 resource server: every API request needs a DIAM client-credentials access token (Bearer JWT).
 * Spring Boot's JwtDecoder checks the signature against DIAM's JWKS, the issuer, the timestamps and the optional
 * audience; the validator beans below add the azp allow-list and the access-token type check.
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    static final String AUTHORIZED_PARTY_CLAIM = "azp";
    static final String TOKEN_TYPE_CLAIM = "typ";
    static final String ACCESS_TOKEN_TYPE = "Bearer";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, DigitalFormsAuthenticationFailureHandler failureHandler,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/v3/api-docs/**"
                                , "/v3/api-docs.yaml"
                                , "/swagger-resources*"
                                , "/swagger-ui.html"
                                , "/swagger-ui/**"
                                , "/webjars/**"
                                , "/swagger-config/**"
                                , "/actuator/health"
                        ).permitAll().anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(withDefaults())
                        .authenticationEntryPoint(failureHandler)
                        // RFC 9728 metadata at /.well-known/oauth-protected-resource names DIAM as the token issuer.
                        // Tokens are not certificate-bound because TLS terminates at the OpenShift route.
                        .protectedResourceMetadata(metadata -> metadata.protectedResourceMetadataCustomizer(
                                resource -> resource.authorizationServer(issuer)
                                        .tlsClientCertificateBoundAccessTokens(false)))
                        // The default handler rethrows when DIAM's keys cannot be fetched; answer with the API's 500 JSON.
                        .withObjectPostProcessor(new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
                            @Override
                            public <O extends BearerTokenAuthenticationFilter> O postProcess(O filter) {
                                filter.setAuthenticationFailureHandler(failureHandler);
                                return filter;
                            }
                        }));
        return http.build();
    }

    /** Only the configured DIAM clients may call the API; the realm also issues tokens to unrelated clients. */
    @Bean
    OAuth2TokenValidator<Jwt> authorizedClientValidator(
            @Value("${digitalforms.oauth2.allowed-client-ids}") String allowedClientIds) {
        Set<String> allowed = Arrays.stream(allowedClientIds.split(","))
                .map(String::trim)
                .filter(clientId -> !clientId.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        if (allowed.isEmpty()) {
            throw new IllegalStateException("digitalforms.oauth2.allowed-client-ids (DIGITALFORMS_OAUTH2_ALLOWED_CLIENT_IDS)"
                    + " must list at least one DIAM client ID");
        }
        return new JwtClaimValidator<String>(AUTHORIZED_PARTY_CLAIM, azp -> azp != null && allowed.contains(azp));
    }

    /** Rejects Keycloak ID tokens, which are signed with the same keys but are not access tokens. */
    @Bean
    OAuth2TokenValidator<Jwt> accessTokenTypeValidator() {
        return new JwtClaimValidator<String>(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE::equals);
    }
}