package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.DocumentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.model.AssociateDocumentToNoticeServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetDocumentsListServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.StoreVIPSDocument;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsDocumentResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsGetDocumentByIdResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsNoticeObj;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import ca.bc.gov.open.jag.ordsvipsclient.api.DocumentApi;
import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiException;
import ca.bc.gov.open.jag.ordsvipsclient.api.model.VipsDocumentOrdsResponse;

@Service
public class DocumentsApiDelegateImpl implements DocumentsApiDelegate{
	
	@Autowired
	private DocumentApi documentApi;
	
	@Autowired
	private  ConfigProperties properties;
	
	private final Logger logger = LoggerFactory.getLogger(DocumentsApiDelegateImpl.class);
	
	@Autowired
	private VipsRestService digitalformsApiService;
	
	@Override
	public ResponseEntity<GetDocumentsListServiceResponse> documentsListNoticeNoCorrelationIdGet(String noticeNo,
	        String correlationId) {
		
		logger.info("Heard a call to the endpoint 'documentsListNoticeNoCorrelationIdGet' with noticeNo " + noticeNo);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	/**
	 * Create Document / Notice Number Association 
	 * 
	 * Possible Response codes:
	 * 	201. Created.
	 *  404. Not Found 
	 * 	401. Unauth (digital forms basic auth failure)
	 * 	500. Expected from the SSG if VIPS WS is unavailable or responds with any code other than 200. 
	 * @throws ApiException 
	 */
	@Override
	public ResponseEntity<AssociateDocumentToNoticeServiceResponse> documentsAssociationNoticeDocumentIdCorrelationIdPost(
			String correlationId,
	        Long documentId,
	        VipsNoticeObj body) {
		
		logger.info("Heard a call to the endpoint 'documentsAssociationNoticeDocumentIdCorrelationIdPost' with documentId " + documentId);
		
		AssociateDocumentToNoticeServiceResponse resp = new AssociateDocumentToNoticeServiceResponse();
		
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse _resp = 
				digitalformsApiService.createDocumentAsociationPost(
																documentId, 
																body);
		
		// Depending on the result code from the VIPS store document call, set the response entity accordingly. 
		if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			resp.setStatusMessage(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD && 
				_resp.getRespMsg().equalsIgnoreCase(DigitalFormsConstants.VIPS_NOTICE_NOT_FOUND)) {
			throw new ResourceNotFoundException("Notice " + body.getNoticeNo() + " and notice type: " + 
				body.getNoticeTypeCd() + " not found in VIPS.");
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
			logger.error("VIPS Error: " + _resp.getRespMsg());
			throw new DigitalFormsException("Failed to create document association between document id: " + documentId + 
					" and notice number " + body.getNoticeNo() + " and notice type: " + body.getNoticeTypeCd()); 
		}
		
		return new ResponseEntity<>(resp, HttpStatus.CREATED);
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
						properties.getVipsRestApiCredentialsGuid(),		//required  Note: Uses same auth as VIPS WS API. 
						storeVIPSDocument.getFileObject(), 				//required 
						storeVIPSDocument.getNoticeTypeCode(),			//optional 
						storeVIPSDocument.getNoticeSubjectCode(),		//optional
						storeVIPSDocument.getPageCount());				//optional
			
			// Depending on the result code from the VIPS store document call, set the response entity accordingly. 
			if (_resp.getStatusCode().equals(DigitalFormsConstants.VIPSORDS_SUCCESS_CD)) {
				resp.setDocumentId(_resp.getDocumentId());
			} else if (_resp.getStatusCode().equals(DigitalFormsConstants.VIPSORDS_GENERAL_FAILURE_CD)) {
				logger.error("VIPS Error: " + _resp.getStatusMessage());
				throw new DigitalFormsException("Failed to store document document to VIPS WS. Type Code : " + storeVIPSDocument.getTypeCode() + 
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
	public ResponseEntity<VipsGetDocumentByIdResponse> documentsDocumentIdCorrelationIdGet(
			String correlationId,
	        Long documentId) {
		
		logger.info("Heard a call to the endpoint 'documentsDocumentIdCorrelationIdGet' with documentId " + documentId);
		
		VipsGetDocumentByIdResponse documentResponse = digitalformsApiService.getDocumentAsBase64(correlationId, documentId);
		
		if (documentResponse != null && documentResponse.getDocument() == null || documentResponse.getDocument().isEmpty()) {
			logger.error("VIPS Error: Could not return a document base64 for the provided documentID: " + documentId);
			throw new ResourceNotFoundException("Document Not Found");
		} else if (!Base64.isBase64(documentResponse.getDocument())) {
			logger.error("VIPS Error: Invalid base64 format returned for the provided documentID: " + documentId);
			throw new DigitalFormsException("Invalid base64 string");
		}
		
		return new ResponseEntity<>(documentResponse, HttpStatus.OK);
	}

}
