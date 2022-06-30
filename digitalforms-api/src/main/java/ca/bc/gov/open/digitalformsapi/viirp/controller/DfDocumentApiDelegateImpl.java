package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.DfDocumentApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetDFPayloadServiceResponse;

@Service
public class DfDocumentApiDelegateImpl implements DfDocumentApiDelegate{
	
	@Override
	public ResponseEntity<GetDFPayloadServiceResponse> dfDocumentDfIdCorrelationIdGet(Long dfId,
	        String correlationId) {
		
		System.out.println("Heard a call to the endpoint 'dfDocumentDfIdCorrelationIdGet' with dfId " + dfId + " and correlationId " + correlationId);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
