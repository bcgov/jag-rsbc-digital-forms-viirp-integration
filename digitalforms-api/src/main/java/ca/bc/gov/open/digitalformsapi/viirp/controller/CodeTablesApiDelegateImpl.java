package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.CodetablesApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;

@Service
public class CodeTablesApiDelegateImpl implements CodetablesApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(CodeTablesApiDelegateImpl.class);
	
	@Override
	public ResponseEntity<GetCodetablesServiceResponse> codetablesCorrelationIdGet(String correlationId) {
		
		logger.info("Heard a call to the endpoint 'codetablesCorrelationIdGet'");
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
