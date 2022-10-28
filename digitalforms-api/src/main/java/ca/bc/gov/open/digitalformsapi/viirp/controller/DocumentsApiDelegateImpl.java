package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientException;

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
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse;
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
	
	/**
	 * GET Documents Meta Data (From VIPS WS) 
	 * 
	 * Response from this method is a cascade of two calls: 
	 * 	1.) Call to VIPS WS to locate the impoundmentId or prohibitionId associated with the given notice number. 
	 * 	2.) Call VIPS WS with the impoudnmentId or prohibitionId to get the list of documents meta data object (if the above provides an id).  
	 * 
	 * Possible Response codes:
	 * 	200. Success. Documents meta data found and returned. 
	 * 	401. unauth (digital forms basic auth failure)
	 *  404. Not found (Initial search call failed to find impoundment or prohibition for given notice number)
	 * 	500. from the SSG if VIPS WS is unavailable or responds with any code other than 200. 
	 */
	@Override
	public ResponseEntity<GetDocumentsListServiceResponse> documentsListNoticeNoCorrelationIdGet(String noticeNo,
	        String correlationId) {
		
		logger.info("Heard a call to the endpoint 'documentsListNoticeNoCorrelationIdGet' with noticeNo " + noticeNo);
		
		GetDocumentsListServiceResponse documentsListByImpoundmentResp = new GetDocumentsListServiceResponse();
		
		// Start cascade by fetching impoundmentId for notice number. 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse impoundmentSearchResp = new SearchImpoundmentsServiceResponse();
		try {
			impoundmentSearchResp = digitalformsApiService.searchImpoundment(correlationId, noticeNo);
		} catch (WebClientException e) {
			logger.error("VIPS Internal Server Error: " + e.getMessage());
			throw new DigitalFormsException("Internal Server Error at VIPS WS. Failed to search impoundment with noticeNo : " + noticeNo);
		}
		
		if (impoundmentSearchResp.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD && !(null == impoundmentSearchResp.getResult() || impoundmentSearchResp.getResult().isEmpty())) {
			
			// This shouldn't happen - but if it does, we're ready. 
			if (impoundmentSearchResp.getResult().size() > 1) {
					throw new DigitalFormsException("Unexpected impoundment search results received from VIPS WS. Results > 1 for Notice No: " + noticeNo);
			}
			 
			Long impoundmentId = impoundmentSearchResp.getResult().get(0).getImpoundmentId();
			
			logger.info("Attempting to retrieve the documents by impoundment id: " + impoundmentId);
				
			// Continue cascade to fetch List of Documents for impoundment id. 
			ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetDocumentsListServiceResponse _resp2 = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetDocumentsListServiceResponse();
			try {
				_resp2 = digitalformsApiService.getDocumentsMetaList(correlationId, impoundmentId, null);
			} catch (WebClientException e) {
				logger.error("VIPS Internal Server Error: " + e.getMessage());
				throw new DigitalFormsException("Internal Server Error at VIPS WS. Failed to get documents list with impoundmentId : " + impoundmentId);
			}
				
			// Depending on the result code from VIPS, we set the response Entity accordingly. 
			if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
				
				// Make sure response has the expected documents detail. If this fails we have a bigger problem in VIPS
				// as the prior VIPS search said that there was an impoundment for the id returned above. 
				if (null != _resp2.getResults() && impoundmentSearchResp.getResult().size() > 0) {
				
					// Populate response object from VIPS response object (This removes the VIPS WS resultCd, resultMsg, etc.) 
					documentsListByImpoundmentResp.setResults(_resp2.getResults());
					
				} else {
					throw new ResourceNotFoundException("No documents returned when a prior search indicated there were documents linked to impoundment id  : " + impoundmentId);
				}
				
			} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
				logger.error("VIPS error response code: " + _resp2.getRespCd() + ", response msg:  " + _resp2.getRespMsg());
				throw new DigitalFormsException("Failed to get documents for impoundmentId : " + impoundmentId);
			} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
				logger.error("VIPS error response code: " + _resp2.getRespCd() + ", response msg:  " + _resp2.getRespMsg());
				throw new DigitalFormsException("Internal Java error at VIPS WS. Failed to get documents for impoundment Id : " + impoundmentId);
			}
			
			return new ResponseEntity<>(documentsListByImpoundmentResp, HttpStatus.OK);
			
		} else {
			
			GetDocumentsListServiceResponse documentsListByProhibitionResp = new GetDocumentsListServiceResponse();
			
			// Start cascade by fetching prohibitionId for notice number. 
			ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse prohibitionSearchResp = new SearchProhibitionsServiceResponse();
			try {
				prohibitionSearchResp = digitalformsApiService.searchProhibition(correlationId, noticeNo);
			} catch (WebClientException e) {
				logger.error("VIPS Internal Server Error: " + e.getMessage());
				throw new DigitalFormsException("Internal Server Error at VIPS WS. Failed to search prohibition with noticeNo : " + noticeNo);
			}
			
			if (prohibitionSearchResp.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			
				if (null == prohibitionSearchResp.getResult() || prohibitionSearchResp.getResult().isEmpty()) {
					throw new ResourceNotFoundException("Not Found"); 
				} 
				
				// This shouldn't happen - but if it does, we're ready. 
				if (prohibitionSearchResp.getResult().size() > 1) {
						throw new DigitalFormsException("Unexpected prohibition search results received from VIPS WS. Results > 1 for Notice No: " + noticeNo);
				}
				 
				Long prohibitionId = prohibitionSearchResp.getResult().get(0).getProhibitionId();
				
				logger.info("Attempting to retrieve the documents by prohibition id: " + prohibitionId);
					
				// Continue cascade to fetch List of Documents for prohibition id. 
				ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetDocumentsListServiceResponse _resp2 = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetDocumentsListServiceResponse();
				try {
					_resp2 = digitalformsApiService.getDocumentsMetaList(correlationId, null, prohibitionId);
				} catch (WebClientException e) {
					logger.error("VIPS Internal Server Error: " + e.getMessage());
					throw new DigitalFormsException("Internal Server Error at VIPS WS. Failed to get documents list with prohibitionId : " + prohibitionId);
				}
				
				// Depending on the result code from VIPS, we set the response Entity accordingly. 
				if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
					
					// Make sure response has the expected documents detail. If this fails we have a bigger problem in VIPS
					// as the prior VIPS search said that there was an prohibition for the id returned above. 
					if (null != _resp2.getResults() && prohibitionSearchResp.getResult().size() > 0) {
					
						// Populate response object from VIPS response object (This removes the VIPS WS resultCd, resultMsg, etc.) 
						documentsListByProhibitionResp.setResults(_resp2.getResults());
						
					} else {
						throw new ResourceNotFoundException("No documents returned when a prior search indicated there were documents linked to prohibition id  : " + prohibitionId);
					}
					
				} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
					logger.error("VIPS error response code: " + _resp2.getRespCd() + ", response msg:  " + _resp2.getRespMsg());
					throw new DigitalFormsException("Failed to get documents for prohibitionId : " + prohibitionId);
				} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
					logger.error("VIPS error response code: " + _resp2.getRespCd() + ", response msg:  " + _resp2.getRespMsg());
					throw new DigitalFormsException("Internal Java error at VIPS WS. Failed to get documents for prohibition Id : " + prohibitionId);
				}	
				
			} else if (prohibitionSearchResp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
				logger.error("VIPS error response code: " + prohibitionSearchResp.getRespCd() + ", response msg:  " + prohibitionSearchResp.getRespMsg());
				throw new DigitalFormsException("Failed to get documents for Notice Number : " + noticeNo);
			} else if (prohibitionSearchResp.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
				logger.error("VIPS error response code: " + prohibitionSearchResp.getRespCd() + ", response msg:  " + prohibitionSearchResp.getRespMsg());
				throw new DigitalFormsException("Internal Java error at VIPS WS. Failed to get documents for Notice Number : " + noticeNo);
			}
			
			return new ResponseEntity<>(documentsListByProhibitionResp, HttpStatus.OK);
		}
		
		
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
		
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse _resp = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse();
		try {
			_resp = digitalformsApiService.createDocumentAsociationPost(
															documentId, 
															body);
		} catch (WebClientException e) {
			logger.error("VIPS Internal Server Error: " + e.getMessage());
			throw new DigitalFormsException("Internal Server Error at VIPS WS. Failed to create document association with documentId : " + documentId);
		}
		
		// Depending on the result code from the VIPS store document call, set the response entity accordingly. 
		if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			resp.setStatusMessage(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD && 
				_resp.getRespMsg().equalsIgnoreCase(DigitalFormsConstants.VIPS_NOTICE_NOT_FOUND)) {
			throw new ResourceNotFoundException("Notice " + body.getNoticeNo() + " and notice type: " + 
				body.getNoticeTypeCd() + " not found in VIPS.");
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
			logger.error("VIPS error response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
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
				logger.error("VIPS error response code: " + _resp.getStatusCode() + ", response msg:  " + _resp.getStatusMessage());
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
		
		VipsGetDocumentByIdResponse documentResponse;
		try {
			documentResponse = digitalformsApiService.getDocumentAsBase64(correlationId, documentId);
		} catch (WebClientException e) {
			logger.error("VIPS Internal Server Error: " + e.getMessage());
			throw new DigitalFormsException("Internal Server Error at VIPS WS. Failed to get base64 format document with documentId : " + documentId);
		}
		
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
