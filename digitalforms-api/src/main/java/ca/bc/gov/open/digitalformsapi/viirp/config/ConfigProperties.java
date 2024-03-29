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
//@Component
//@ConfigurationProperties
@Component
@ConfigurationProperties
public class ConfigProperties {
	
    @Value("${digitalforms.app.version}")
    private String digitalformsAppVersion;

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
    
    @Value("${vips.rest-api.credentials.guid}")
    private String vipsRestApiCredentialsGuid;
    
    @Value("${vips.rest-api.credentials.user}")
    private String vipsRestApiCredentialsUser;
    
    @Value("${vips.rest-api.credentials.displayname}")
    private String vipsRestApiCredentialsDisplayname;
    
    @Value("${digitalforms.basic-auth.user}")
    private String digitalFormsBasicAuthUser;

	@Value("${digitalforms.basic-auth.password}")
	private String digitalFormsBasicAuthPassword;
	
	
	public String getDigitalformsAppVersion() {
		return digitalformsAppVersion;
	}

	public void setDigitalformsAppVersion(String digitalformsAppVersion) {
		this.digitalformsAppVersion = digitalformsAppVersion;
	}

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

	public String getVipsRestApiCredentialsGuid() {
		return vipsRestApiCredentialsGuid;
	}

	public void setVipsRestApiCredentialsGuid(String vipsRestApiCredentialsGuid) {
		this.vipsRestApiCredentialsGuid = vipsRestApiCredentialsGuid;
	}

	public String getVipsRestApiCredentialsUser() {
		return vipsRestApiCredentialsUser;
	}

	public void setVipsRestApiCredentialsUser(String vipsRestApiCredentialsUser) {
		this.vipsRestApiCredentialsUser = vipsRestApiCredentialsUser;
	}

	public String getVipsRestApiCredentialsDisplayname() {
		return vipsRestApiCredentialsDisplayname;
	}

	public void setVipsRestApiCredentialsDisplayname(String vipsRestApiCredentialsDisplayname) {
		this.vipsRestApiCredentialsDisplayname = vipsRestApiCredentialsDisplayname;
	}
	
    public String getDigitalFormsBasicAuthUser() {
		return digitalFormsBasicAuthUser;
	}

	public void setDigitalFormsBasicAuthUser(String digitalFormsBasicAuthUser) {
		this.digitalFormsBasicAuthUser = digitalFormsBasicAuthUser;
	}

	public String getDigitalFormsBasicAuthPassword() {
		return digitalFormsBasicAuthPassword;
	}

	public void setDigitalFormsBasicAuthPassword(String digitalFormsBasicAuthPassword) {
		this.digitalFormsBasicAuthPassword = digitalFormsBasicAuthPassword;
	}
    
}
