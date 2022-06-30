package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.ImpoundmentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetImpoundmentServiceResponse;

@Service
public class ImpoundmentsApiDelegateImpl implements ImpoundmentsApiDelegate{
	
	@Override
	public ResponseEntity<CreateImpoundmentServiceResponse> impoundmentsCorrelationIdPost(String correlationId,
	        CreateImpoundment createImpoundment) {
		
		System.out.println("Heard a call to the endpoint 'impoundmentsCorrelationIdPost' with correlationId " + correlationId + 
				" and CreateImpoundment " + createImpoundment.toString());
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<GetImpoundmentServiceResponse> impoundmentsImpoundmentIdCorrelationIdGet(Integer impoundmentId,
	        String correlationId) {
		
		System.out.println("Heard a call to the endpoint 'impoundmentsImpoundmentIdCorrelationIdGet' with impoundmentId " + impoundmentId + 
				" and correlationId " + correlationId);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
