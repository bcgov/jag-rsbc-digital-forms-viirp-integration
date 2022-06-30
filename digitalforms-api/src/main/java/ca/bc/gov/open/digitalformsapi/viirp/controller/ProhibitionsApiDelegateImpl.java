package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.ProhibitionsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetProhibitionServiceResponse;

@Service
public class ProhibitionsApiDelegateImpl implements ProhibitionsApiDelegate{
	
	@Override
	public ResponseEntity<CreateProhibitionServiceResponse> prohibitionsCorrelationIdPost(String correlationId,
	        CreateProhibition createProhibition) {
		
		System.out.println("Heard a call to the endpoint 'prohibitionsCorrelationIdPost' with correlationId " + correlationId + 
				" and CreateProhibition " + createProhibition.toString());
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<GetProhibitionServiceResponse> prohibitionsProhibitionIdCorrelationIdGet(Integer prohibitionId,
	        String correlationId) {
		
		System.out.println("Heard a call to the endpoint 'prohibitionsProhibitionIdCorrelationIdGet' with prohibitionId " + prohibitionId + 
				" and correlationId " + correlationId);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
