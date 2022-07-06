package ca.bc.gov.open.digitalformsapi.viirp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {
	
	@Bean
	public OpenAPI vipsIntegrationOpenAPI() {
		return new OpenAPI().info(new Info()
		          .title("VIPS Integration API")
		          .description("VIPS Integration API")
		          .version("v1.0"));
	}

}
