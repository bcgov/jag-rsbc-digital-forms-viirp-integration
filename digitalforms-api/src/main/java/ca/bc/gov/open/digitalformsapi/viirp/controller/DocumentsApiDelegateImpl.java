package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.DocumentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.AssociateDocumentToNoticeServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetDocumentsListServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.StoreVIPSDocument;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsDocumentResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsNoticeObj;

@Service
public class DocumentsApiDelegateImpl implements DocumentsApiDelegate{
	
	@Override
	public ResponseEntity<GetDocumentsListServiceResponse> documentsListCorrelationIdGet(
			String correlationId,
	        Long impoundmentId,
	        Long prohibitionId) {
		
		System.out.println("Heard a call to the endpoint 'documentsListCorrelationIdGet' with correlationId " + correlationId + 
				", impoundmentId " + impoundmentId + " and prohibitionId " + prohibitionId);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<AssociateDocumentToNoticeServiceResponse> documentsAssociationNoticeDocumentIdCorrelationIdPost(
			String correlationId,
	        Long documentId,
	        VipsNoticeObj body) {
		
		System.out.println("Heard a call to the endpoint 'documentsAssociationNoticeDocumentIdCorrelationIdPost' with correlationId " + correlationId + 
				", documentId " + documentId + " and VipsNoticeObj " + body.toString());
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<VipsDocumentResponse> documentsCorrelationIdPost(
			String correlationId,
	        StoreVIPSDocument storeVIPSDocument) {
		
		System.out.println("Heard a call to the endpoint 'documentsCorrelationIdPost' with correlationId " + correlationId + 
				" and VipsNoticeObj " + storeVIPSDocument.toString());
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<VipsDocumentResponse> documentsDocumentIdCorrelationIdGet(
			String correlationId,
	        Long documentId,
	        Boolean b64,
	        Boolean url) {
		
		System.out.println("Heard a call to the endpoint 'documentsDocumentIdCorrelationIdGet' with correlationId " + correlationId + 
				", documentId " + documentId + ", b64 " + b64 + ", url " + url);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

}
