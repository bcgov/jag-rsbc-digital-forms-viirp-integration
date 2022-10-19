package ca.bc.gov.open.digitalformsapi.viirp.utils;

public class DFBooleanUtils {
	
	/**
	 * Utility method for converting between boolean and boolean string value
	 * 
	 * @param value
	 * @return
	 */
	public static String getYNFromBoolean(boolean value) {
		if (value)
			return "Y";
		else 
			return "N";
	}
	
	/**
	 * Utility method for converting between boolean string value and boolean
	 * 
	 * @param value
	 * @return
	 */
	public static boolean getBooleanFromYN(String value) {
		if (null == value) 
			return false;
		else if (value.equalsIgnoreCase("Y") || value.equalsIgnoreCase("YES"))
			return true; 
		else 
			return false;
	}

}
