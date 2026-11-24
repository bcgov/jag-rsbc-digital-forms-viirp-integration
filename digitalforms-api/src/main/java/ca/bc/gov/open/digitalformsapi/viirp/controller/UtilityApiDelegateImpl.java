package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.UtilityApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.UpstreamServiceException;
import ca.bc.gov.open.digitalformsapi.viirp.model.PingResponse;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsOrdsService;
import tools.jackson.databind.node.JsonNodeFactory;

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
	private VipsOrdsService vipsOrdsService;
		
	@Override
	public ResponseEntity<PingResponse> pingOrds(String xCorrelationID) {
		
		logger.info("Heard a call to 'pingOrds'");
		
		PingResponse resp = new PingResponse();
		var health = vipsOrdsService.health();
		if (health == null || health.getStatus() == null || health.getStatus().isBlank()) {
			throw UpstreamServiceException.invalidResponse("VIPS ORDS returned no health status.");
		}
		resp.setResponseMessage(JsonNodeFactory.instance.objectNode()
				.put("VIPS ORDS Health Status", health.getStatus()));
		return new ResponseEntity<>(resp, HttpStatus.OK);
	}
}
