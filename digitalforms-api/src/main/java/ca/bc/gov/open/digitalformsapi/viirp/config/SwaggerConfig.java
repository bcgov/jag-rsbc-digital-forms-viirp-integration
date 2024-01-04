package ca.bc.gov.open.digitalformsapi.viirp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {
	
    @Autowired
    private ConfigProperties properties;
    
    @Bean
    public OpenAPI apiDocConfig() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Forms VI IRP API")
                        .description("VIPS VI IRP Integration API")
                        .version("v" + properties.getDigitalformsAppVersion()));
    }

}
