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
	
	// VIPS ORDS Response code values
	public static final String VIPSORDS_SUCCESS_CD = "0";
	public static final String VIPSORDS_GENERAL_FAILURE_CD = "1";
	
	//Digital Forms message constants
	public static final String DIGITALFORMS_SUCCESS_MSG = "success";
	
	//VIPS WS Constants
	public static final String ICBC_DATE_FORMAT = "yyyy-MM-dd";
	public static final String ICBC_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String VIPS_DISPLAY_DATE_FORMAT = "yyyy-MM-dd";
	
	public static final String VIPS_NOTICE_NOT_FOUND = "Notice not found";
	
	//Unit test constants
	public static final String UNIT_TEST_CORRELATION_ID = "1234567";
	public static final String UNIT_TEST_NOTICE_NUMBER = "22222222";
	public static final Long UNIT_TEST_IMPOUNDMENT_ID = 1234L;
	public static final Long UNIT_TEST_DOCUMENT_ID = 1234L;
	
	
	
}
