package ca.bc.gov.open.digitalformsapi.viirp.config;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;

import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;

@Configuration(proxyBeanMethods = false)
public class MultipartOpenApiConfig {

    /** Security scheme name used by VI_IRP_V1.yaml. */
    public static final String SECURITY_SCHEME = "oauth2";
    static final String SECURITY_SCHEME_DESCRIPTION = "DIAM (Keycloak) OAuth 2.0 client credentials. Request an access"
            + " token from the token URL with the client ID and secret issued for your environment, then send it in the"
            + " Authorization header as \"Bearer <access_token>\". Tokens are short-lived; request a new token when the"
            + " current one expires.";

    @Bean
    OperationCustomizer requiredMultipartFields() {
        return (operation, handler) -> {
            var body = operation.getRequestBody();
            if (body == null || body.getContent() == null) {
                return operation;
            }
            var media = body.getContent().get(MediaType.MULTIPART_FORM_DATA_VALUE);
            if (media == null || media.getSchema() == null || media.getSchema().getProperties() == null) {
                return operation;
            }
            // springdoc 3.2 moves RequestParam fields into the form but drops their required flags.
            for (var parameter : handler.getMethodParameters()) {
                var annotation = parameter.getParameterAnnotation(RequestParam.class);
                if (annotation != null && annotation.required()
                        && ValueConstants.DEFAULT_NONE.equals(annotation.defaultValue())
                        && media.getSchema().getProperties().containsKey(annotation.value())) {
                    media.getSchema().addRequiredItem(annotation.value());
                }
            }
            return operation;
        };
    }

    @Bean
    OpenApiCustomizer actuatorAuthenticationRequirements() {
        return api -> {
            if (api.getPaths() == null) {
                return;
            }
            for (String path : new String[] {"/actuator", "/actuator/info"}) {
                var pathItem = api.getPaths().get(path);
                if (pathItem != null && pathItem.getGet() != null) {
                    pathItem.getGet().setSecurity(java.util.List.of(new SecurityRequirement().addList(SECURITY_SCHEME)));
                }
            }
        };
    }

    /** The generated SpringDocConfiguration omits the OAuth2 flow, which clients such as Postman need for the token URL. */
    @Bean
    OpenApiCustomizer oauth2ClientCredentialsFlow(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
        return api -> {
            if (api.getComponents() == null || api.getComponents().getSecuritySchemes() == null) {
                return;
            }
            var scheme = api.getComponents().getSecuritySchemes().get(SECURITY_SCHEME);
            if (scheme != null) {
                scheme.description(SECURITY_SCHEME_DESCRIPTION).flows(new OAuthFlows().clientCredentials(new OAuthFlow()
                        .tokenUrl(issuer + "/protocol/openid-connect/token").scopes(new Scopes())));
            }
        };
    }
}
