package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.UtilityApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsPingServiceResponse;
import ca.bc.gov.open.jag.ordsvipsclient.api.HealthApi;
import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiException;

@Service
public class UtilityApiDelegateImpl implements UtilityApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(UtilityApiDelegateImpl.class);
	
	@Autowired
	private HealthApi vipsHealthApi;
		
	@Override
	public ResponseEntity<VipsPingServiceResponse> utilityVipsordsPingCorrelationIdGet(String correlationId) {
		
		logger.info("Heard a call to the endpoint 'utilityVipsordsPingCorrelationIdGet'");
		
		VipsPingServiceResponse resp = new VipsPingServiceResponse();
		try {
			resp.setStatus(vipsHealthApi.health().getStatus()); 
			resp.setHost(vipsHealthApi.health().getHost());
			resp.setInstance(vipsHealthApi.health().getInstance()); 
						
		} catch (ApiException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
		
		return new ResponseEntity<>(resp, HttpStatus.OK);
	}
}
