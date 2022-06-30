package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.DocumentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetDocumentsServiceResponse;

@Service
public class DocumentsApiDelegateImpl implements DocumentsApiDelegate{
	
	@Override
	public ResponseEntity<GetDocumentsServiceResponse> documentsCorrelationIdGet(String correlationId,
	        Long impoundmentId,
	        Long prohibitionId) {
		
		System.out.println("Heard a call to the endpoint 'documentsCorrelationIdGet' with correlationId " + correlationId + 
				", impoundmentId " + impoundmentId + " and prohibitionId " + prohibitionId);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
