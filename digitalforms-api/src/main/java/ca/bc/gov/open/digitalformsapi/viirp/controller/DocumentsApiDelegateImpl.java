package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
	private final Logger logger = LoggerFactory.getLogger(DocumentsApiDelegateImpl.class);
	
	@Override
	public ResponseEntity<GetDocumentsListServiceResponse> documentsListNoticeNoCorrelationIdGet(String noticeNo,
	        String correlationId) {
		
		logger.info("Heard a call to the endpoint 'documentsListNoticeNoCorrelationIdGet' with noticeNo " + noticeNo);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<AssociateDocumentToNoticeServiceResponse> documentsAssociationNoticeDocumentIdCorrelationIdPost(
			String correlationId,
	        Long documentId,
	        VipsNoticeObj body) {
		
		logger.info("Heard a call to the endpoint 'documentsAssociationNoticeDocumentIdCorrelationIdPost' with documentId " + documentId);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<VipsDocumentResponse> documentsCorrelationIdPost(
			String correlationId,
	        StoreVIPSDocument storeVIPSDocument) {
		
		logger.info("Heard a call to the endpoint 'documentsCorrelationIdPost'");
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<VipsDocumentResponse> documentsDocumentIdCorrelationIdGet(
			String correlationId,
	        Long documentId,
	        Boolean b64,
	        Boolean url) {
		
		logger.info("Heard a call to the endpoint 'documentsDocumentIdCorrelationIdGet' with documentId " + documentId + ", b64 " + b64 + ", url " + url);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

}
