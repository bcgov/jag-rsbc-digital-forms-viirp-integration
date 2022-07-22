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
	
	@Autowired
	private ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.HealthApi digitalFormsHealthApi;
		
	@SuppressWarnings("unchecked")
	@Override
	public ResponseEntity<PingResponse> utilityOrdsPingCorrelationIdGet(String correlationId) {
		
		logger.info("Heard a call to 'utilityOrdsPingCorrelationIdGet'");
		
		PingResponse resp = new PingResponse();
		try {
			
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("VIPS ORDS Health Status", vipsHealthApi.health().getStatus());
			jsonResponse.put("DIGITAL FORMS ORDS Health Status", digitalFormsHealthApi.health().getStatus());
			resp.setResponseMessage(jsonResponse);
			
			return new ResponseEntity<>(resp, HttpStatus.OK); 
						
		} catch (ApiException e) {
			String msg = "Error encountered while pinging VIPS ORDS Health endpoint: " + e.getMessage();
			e.printStackTrace();
			throw new DigitalFormsException(msg, e);
			
		} catch (ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.handler.ApiException  e) {
			String msg = "Error encountered while pinging DigitalForms ORDS Health endpoints: " + e.getMessage();
			e.printStackTrace();
			throw new DigitalFormsException(msg, e);
			
		}
	}
}
