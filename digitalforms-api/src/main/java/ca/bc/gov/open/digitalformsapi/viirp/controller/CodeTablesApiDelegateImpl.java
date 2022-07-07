package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.CodetablesApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;

@Service
public class CodeTablesApiDelegateImpl implements CodetablesApiDelegate{
	
	@Override
	public ResponseEntity<GetCodetablesServiceResponse> codetablesCorrelationIdGet(String correlationId) {
		
		System.out.println("Heard a call to the endpoint 'codetablesCorrelationIdGet' with correlationId " + correlationId);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
