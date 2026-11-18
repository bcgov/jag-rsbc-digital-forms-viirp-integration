package ca.bc.gov.open.digitalformsapi.viirp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.bc.gov.open.digitalformsapi.viirp.utils.TemporalJson;
import tools.jackson.databind.JacksonModule;

@Configuration(proxyBeanMethods = false)
public class JacksonConfiguration {

    /** Spring Boot registers module beans on the MVC {@code JsonMapper} after any discovered modules. */
    @Bean
    JacksonModule publicTemporalJsonModule() {
        return TemporalJson.publicModule();
    }
}
