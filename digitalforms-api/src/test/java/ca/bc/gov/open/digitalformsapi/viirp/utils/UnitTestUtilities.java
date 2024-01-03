package ca.bc.gov.open.digitalformsapi.viirp.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author 176899
 *
 */
public class UnitTestUtilities {
	
	/**
     * Utility method to convert JSOB object to string. 
     * @param obj
     * @return
     */
    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
