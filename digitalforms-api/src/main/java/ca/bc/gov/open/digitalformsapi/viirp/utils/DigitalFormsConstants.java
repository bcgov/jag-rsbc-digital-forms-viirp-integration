package ca.bc.gov.open.digitalformsapi.viirp.utils;

/**
 * 
 * @author 176899
 *
 */
public final class DigitalFormsConstants {
	
	// MDC constants
	public static final String REQUEST_CORRELATION_ID = "correlationId";
	
	// Exception constants
	public static final String EXCEPTION_NOT_FOUND = "Not Found";	//404
	public static final String EXCEPTION_UNAUTHORIZED = "Unauthorized"; //401
	public static final String EXCEPTION_FORBIDDEN = "Forbidden"; //403
	public static final String EXCEPTION_DIGITAL_FORMS_TEST = "Digital Forms Error"; //500
	
	
	// Custom Headers for VIPS API Calls
	public static final String VIPS_API_HEADER_GUID = "smgov_userguid";
	public static final String VIPS_API_HEADER_DISPLAYNAME = "smgov_userdisplayname";
	public static final String VIPS_API_HEADER_USER = "sm_user";
	
	
	// VIPS General Response code values
	public static final int VIPSWS_SUCCESS_CD = 0;
	public static final int VIPSWS_GENERAL_FAILURE_CD = 1;
	public static final int VIPSWS_JAVA_EX = 99; // low level JAVA issue rather than a DB problem.
	
	//Digital Forms message constants
	public static final String DIGITALFORMS_SUCCESS_MSG = "success";
	
	
}
