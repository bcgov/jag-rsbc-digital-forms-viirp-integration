package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.UtilityApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.model.PingResponse;
import ca.bc.gov.open.jag.ordsvipsclient.api.HealthApi;
import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiException;

/**
 * 
 * Utility Delegate. ORDS Ping service, etc.  
 * 
 * @author 176899
 *
 */
@Service
public class UtilityApiDelegateImpl implements UtilityApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(UtilityApiDelegateImpl.class);
	
	@Autowired
	private HealthApi vipsHealthApi;
		
	@SuppressWarnings("unchecked")
	@Override
	public ResponseEntity<PingResponse> pingOrds(String xCorrelationID) {
		
		logger.info("Heard a call to 'pingOrds'");
		
		PingResponse resp = new PingResponse();
		try {
			
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("VIPS ORDS Health Status", vipsHealthApi.health().getStatus());
			resp.setResponseMessage(jsonResponse);
			
			return new ResponseEntity<>(resp, HttpStatus.OK); 
						
		} catch (ApiException e) {
			String msg = "Error encountered while pinging VIPS ORDS Health endpoint: " + e.getMessage();
			e.printStackTrace();
			logger.error(msg);
			throw new DigitalFormsException(msg, e);
			
		}
	}
}
