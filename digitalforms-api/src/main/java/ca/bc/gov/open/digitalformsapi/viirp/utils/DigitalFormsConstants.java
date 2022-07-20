package ca.bc.gov.open.digitalformsapi.viirp.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author 176899
 *
 */
@Component
@Getter
@Setter
public final class DigitalFormsConstants {
	
	// MDC constants
	public static final String REQUEST_CORRELATION_ID = "correlationId";
	
	// Exception constants
	public static final String EXCEPTION_NOT_FOUND = "Not Found";	//404
	public static final String EXCEPTION_UNAUTHORIZED = "Unauthorized"; //401
	public static final String EXCEPTION_FORBIDDEN = "Forbidden"; //403
	public static final String EXCEPTION_DIGITAL_FORMS_TEST = "Digital Forms Error"; //500
 
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
