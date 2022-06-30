package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.DocumentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.AssociateDocumentToNoticeServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetDocumentsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.StoreVIPSDocument;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsDocumentResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsNoticeObj;

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
	
	@Override
	public ResponseEntity<AssociateDocumentToNoticeServiceResponse> documentAssociationNoticeDocumentIdCorrelationIdPost(String correlationId,
	        Long documentId,
	        VipsNoticeObj body) {
		
		System.out.println("Heard a call to the endpoint 'documentAssociationNoticeDocumentIdCorrelationIdPost' with correlationId " + correlationId + 
				", documentId " + documentId + " and VipsNoticeObj " + body.toString());
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<VipsDocumentResponse> documentCorrelationIdPost(String correlationId, StoreVIPSDocument storeVIPSDocument) {
		
		System.out.println("Heard a call to the endpoint 'documentCorrelationIdPost' with correlationId " + correlationId + 
				" and VipsNoticeObj " + storeVIPSDocument.toString());
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<VipsDocumentResponse> documentDocumentIdCorrelationIdGet(String correlationId, Long documentId, Boolean b64, Boolean url) {
		
		System.out.println("Heard a call to the endpoint 'documentDocumentIdCorrelationIdGet' with correlationId " + correlationId + 
				", documentId " + documentId + ", b64 " + b64 + ", url " + url);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
