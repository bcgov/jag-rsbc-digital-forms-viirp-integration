package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.DocumentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.model.AssociateDocumentToNoticeServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetDocumentsListServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.StoreVIPSDocument;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsDocumentResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsNoticeObj;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import ca.bc.gov.open.jag.ordsvipsclient.api.DocumentApi;
import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiException;
import ca.bc.gov.open.jag.ordsvipsclient.api.model.VipsDocumentOrdsResponse;

@Service
public class DocumentsApiDelegateImpl implements DocumentsApiDelegate{
	
	@Autowired
	private DocumentApi documentApi;
	
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
	
	/**
	 * POST Document (Store document in VIPS) 
	 * 
	 * Possible Response codes:
	 * 	201. created. 
	 * 	401. unauth (digital forms basic auth failure)
	 * 	500. from the SSG if VIPS WS is unavailable or responds with any code other than 200. 
	 * @throws ApiException 
	 */
	@Override
	public ResponseEntity<VipsDocumentResponse> documentsCorrelationIdPost(
			String correlationId,
	        StoreVIPSDocument storeVIPSDocument) {
		
		logger.info("Heard a call to the endpoint 'documentsCorrelationIdPost'");
		
		VipsDocumentResponse resp = new VipsDocumentResponse();
	
		VipsDocumentOrdsResponse _resp;
		try {
			_resp = documentApi.storeDocumentPost(
						storeVIPSDocument.getTypeCode(),				//required
						storeVIPSDocument.getMimeType(), 				//required
						storeVIPSDocument.getMimeSubType(),				//required	 
						storeVIPSDocument.getAuthGuid(),				//required
						storeVIPSDocument.getFileObject(), 				//required 
						storeVIPSDocument.getNoticeTypeCode(),			//optional 
						storeVIPSDocument.getNoticeSubjectCode(),		//optional
						storeVIPSDocument.getPageCount());				//optional
			
			// Depending on the result code from the VIPS store document call, set the response entity accordingly. 
			if (_resp.getStatusCode().equals(String.valueOf(DigitalFormsConstants.VIPSWS_SUCCESS_CD))) {
				resp.setDocumentId(_resp.getDocumentId());
			} else if (_resp.getStatusCode().equals(String.valueOf(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD))) {
				logger.error("VIPS Error: " + _resp.getStatusMessage());
				throw new DigitalFormsException("Failed to store document document to VIPS WS. Type Code : " + storeVIPSDocument.getTypeCode() + 
						". Mime sub type : " + storeVIPSDocument.getMimeSubType() + 
						". Mime type : " + storeVIPSDocument.getMimeType());
			} else if (_resp.getStatusCode().equals(String.valueOf(DigitalFormsConstants.VIPSWS_JAVA_EX))) {
				logger.error("VIPS Error: " + _resp.getStatusMessage());
				throw new DigitalFormsException("Internal Java error at VIPS WS. Failed to store document to VIPS WS. Type Code : " + storeVIPSDocument.getTypeCode() + 
						". Mime sub type : " + storeVIPSDocument.getMimeSubType() +
						". Mime type : " + storeVIPSDocument.getMimeType());
			}
			
			return new ResponseEntity<>(resp, HttpStatus.OK);
			
			
		} catch (ApiException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			throw new DigitalFormsException(e.getMessage(), e);
		}			 
		
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
