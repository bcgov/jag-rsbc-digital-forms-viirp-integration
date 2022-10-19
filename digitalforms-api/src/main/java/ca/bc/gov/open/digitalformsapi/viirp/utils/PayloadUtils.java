package ca.bc.gov.open.digitalformsapi.viirp.utils;

import java.util.LinkedHashMap;

import org.json.simple.JSONObject;


/**
 * Payload conversion utils.
 * 
 * @author 176899
 *
 */
public class PayloadUtils {

	@SuppressWarnings("rawtypes")
	public static String getStringPayloadForMap(Object payload) {
		
		LinkedHashMap<String, String> _payload = (LinkedHashMap) payload;
		JSONObject json = new JSONObject(_payload);
		return json.toJSONString();

	}

}
