package ca.bc.gov.open.digitalformsapi.viirp.config;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;

import io.swagger.v3.oas.models.security.SecurityRequirement;

@Configuration(proxyBeanMethods = false)
public class MultipartOpenApiConfig {

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
                    pathItem.getGet().setSecurity(java.util.List.of(new SecurityRequirement().addList("basicAuth")));
                }
            }
        };
    }
}
