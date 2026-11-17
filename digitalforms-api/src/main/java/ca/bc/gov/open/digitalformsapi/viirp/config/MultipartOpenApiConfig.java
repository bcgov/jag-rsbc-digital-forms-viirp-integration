package ca.bc.gov.open.digitalformsapi.viirp.config;

import java.lang.reflect.Method;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import ca.bc.gov.open.digitalformsapi.viirp.api.ApiUtil;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;

@Configuration(proxyBeanMethods = false)
public class MultipartOpenApiConfig implements WebMvcRegistrations {

    /** Security scheme name used by VI_IRP_V1.yaml. */
    public static final String SECURITY_SCHEME = "oauth2";
    static final String SECURITY_SCHEME_DESCRIPTION = "DIAM (Keycloak) OAuth 2.0 client credentials. Request an access"
            + " token from the token URL with the client ID and secret issued for your environment, then send it in the"
            + " Authorization header as \"Bearer <access_token>\". Tokens are short-lived; request a new token when the"
            + " current one expires.";

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {
            @Override
            protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
                var mapping = super.getMappingForMethod(method, handlerType);
                if (mapping == null || !handlerType.getPackageName().equals(ApiUtil.class.getPackageName())) {
                    return mapping;
                }
                var mediaTypes = mapping.getProducesCondition().getProducibleMediaTypes();
                if (!mediaTypes.contains(MediaType.APPLICATION_PROBLEM_JSON)) {
                    return mapping;
                }
                // Generated produces lists include errors; only success types belong in route negotiation.
                String[] successTypes = mediaTypes.stream()
                        .filter(media -> !MediaType.APPLICATION_PROBLEM_JSON.equals(media))
                        .map(MediaType::toString).toArray(String[]::new);
                return mapping.mutate().produces(successTypes).build();
            }
        };
    }

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
    OperationCustomizer responseMediaTypes() {
        return (operation, handler) -> {
            if (operation.getResponses() == null) {
                return operation;
            }
            // The generator repeats the operation's combined media types on every response annotation.
            operation.getResponses().forEach((status, response) -> {
                var content = response.getContent();
                if (content == null || !content.containsKey(MediaType.APPLICATION_PROBLEM_JSON_VALUE)) {
                    return;
                }
                if (status.startsWith("4") || status.startsWith("5")) {
                    var schema = content.get(MediaType.APPLICATION_PROBLEM_JSON_VALUE).getSchema();
                    if (schema != null && "#/components/schemas/Problem".equals(schema.get$ref())) {
                        content.keySet().removeIf(media -> !MediaType.APPLICATION_PROBLEM_JSON_VALUE.equals(media));
                    }
                } else if (status.startsWith("2")) {
                    content.remove(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                }
            });
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
