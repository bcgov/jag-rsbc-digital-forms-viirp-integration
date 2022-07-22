package ca.bc.gov.open.digitalformsapi.viirp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
/**
 * Externalized configuration for easy access to properties
 * 
 * @author 237563
 *
 */
@Component
@ConfigurationProperties
public class ConfigProperties {

	//Endpoint properties and credentials
    @Value("${vips.rest-api.url}")
    private String vipsRestApiUrl;

    @Value("${vips.rest-api.username}")
    private String vipsRestApiUsername;

    @Value("${vips.rest-api.password}")
    private String vipsRestApiPassword;
    
    @Value("${vips.rest-api.timeout}")
    private int vipsRestApiTimeout;
    
    @Value("${vips.rest-api.retry.count}")
    private long vipsRestApiRetryCount;
    
    @Value("${vips.rest-api.retry.delay}")
    private long vipsRestApiRetryDelay;

	public String getVipsRestApiUrl() {
		return vipsRestApiUrl;
	}

	public void setVipsRestApiUrl(String vipsRestApiUrl) {
		this.vipsRestApiUrl = vipsRestApiUrl;
	}

	public String getVipsRestApiUsername() {
		return vipsRestApiUsername;
	}

	public void setVipsRestApiUsername(String vipsRestApiUsername) {
		this.vipsRestApiUsername = vipsRestApiUsername;
	}

	public String getVipsRestApiPassword() {
		return vipsRestApiPassword;
	}

	public void setVipsRestApiPassword(String vipsRestApiPassword) {
		this.vipsRestApiPassword = vipsRestApiPassword;
	}

	public int getVipsRestApiTimeout() {
		return vipsRestApiTimeout;
	}

	public void setVipsRestApiTimeout(int vipsRestApiTimeout) {
		this.vipsRestApiTimeout = vipsRestApiTimeout;
	}

	public long getVipsRestApiRetryCount() {
		return vipsRestApiRetryCount;
	}

	public void setVipsRestApiRetryCount(long vipsRestApiRetryCount) {
		this.vipsRestApiRetryCount = vipsRestApiRetryCount;
	}

	public long getVipsRestApiRetryDelay() {
		return vipsRestApiRetryDelay;
	}

	public void setVipsRestApiRetryDelay(long vipsRestApiRetryDelay) {
		this.vipsRestApiRetryDelay = vipsRestApiRetryDelay;
	}
    
}
