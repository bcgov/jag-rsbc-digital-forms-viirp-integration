package ca.bc.gov.open.digitalformsapi.viirp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Externalized configuration for easy access to properties
 * 
 * @author 237563
 *
 */
@Component
@Getter
@Setter
public class ConfigProperties {

	//Endpoint properties and credentials
    @Value("${endpoint.vips-rest-api.url}")
    private String vipsRestApiUrl;

    @Value("${endpoint.vips-rest-api.username}")
    private String vipsRestApiUsername;

    @Value("${endpoint.vips-rest-api.password}")
    private String vipsRestApiPassword;
    
    @Value("${endpoint.vips-rest-api.timeout}")
    private int vipsRestApiTimeout;
    
    @Value("${endpoint.vips-rest-api.retry.count}")
    private long vipsRestApiRetryCount;
    
    @Value("${endpoint.vips-rest-api.retry.delay}")
    private long vipsRestApiRetryDelay;
}
