package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.ImpoundmentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsImpoundmentBasicsObj;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@Service
public class ImpoundmentsApiDelegateImpl implements ImpoundmentsApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(ImpoundmentsApiDelegateImpl.class);
	
	@Autowired
	private VipsRestService digitalformsApiService;
	
	/**
	 * POST Impoundment (TO VIPS WS) 
	 * 
	 * Possible Response codes:
	 * 	201. created. 
	 * 	401. unauth (digital forms basic auth failure)
	 * 	500. from the SSG if VIPS WS is unavailable or responds with any code other than 200. 
	 */
	@Override
	public ResponseEntity<CreateImpoundmentServiceResponse> impoundmentsCorrelationIdPost(String correlationId,
	        CreateImpoundment createImpoundment) {
		
		logger.info("Heard a call to the endpoint 'impoundmentsCorrelationIdPost'");
		
		CreateImpoundmentServiceResponse resp = new CreateImpoundmentServiceResponse();
		
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse _resp = digitalformsApiService.createImpoundment(correlationId, createImpoundment);
		
		// Depending on the result code from VIPS, we set the response Entity accordingly. 
		if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			resp.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
			logger.error("VIPS " + _resp.toString());
			throw new DigitalFormsException("Failed to created impoundment for Notice Number : " + createImpoundment.getVipsImpoundCreate().getImpoundmentNoticeNo());
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
			logger.error("VIPS " + _resp.toString());
			throw new DigitalFormsException("Internal Java error at VIPS WS. Failed to created impoundment for Notice Number : " + createImpoundment.getVipsImpoundCreate().getImpoundmentNoticeNo());
		}
		
		return new ResponseEntity<>(resp, HttpStatus.OK);
		
	}

	/**
	 * GET Impoundment (From VIPS WS) 
	 * 
	 * Response from this method is a cascade of two calls: 
	 * 	1.) Call to VIPS WS to locate the impoundmentId associated with the given notice number. 
	 * 	2.) Call VIPS WS with the impoudnmentId to get the impoundment object (if the above provides an id).  
	 * 
	 * Possible Response codes:
	 * 	200. Success. Impoundment found and returned. 
	 * 	401. unauth (digital forms basic auth failure)
	 *  404. Not found (Initial search call failed to find impoundment for given notice number)
	 * 	500. from the SSG if VIPS WS is unavailable or responds with any code other than 200. 
	 */
	@Override
	public ResponseEntity<GetImpoundmentServiceResponse> impoundmentsNoticeNoCorrelationIdGet(String noticeNo,
	        String correlationId) {
		
		logger.info("Heard a call to the endpoint 'impoundmentsImpoundmentIdCorrelationIdGet' with noticeNo " + noticeNo);
		
		GetImpoundmentServiceResponse resp = new GetImpoundmentServiceResponse();
		
		// Start cascade by fetching impoundmentId for notice number. 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse _resp = digitalformsApiService.searchImpoundment(correlationId, noticeNo);
		
		if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
		
			if (null == _resp.getResult() || _resp.getResult().isEmpty()) {
				throw new ResourceNotFoundException("Not Found"); 
			} 
			
			// This shouldn't happen - but if it does, we're ready. 
			if (_resp.getResult().size() > 1) {
					throw new DigitalFormsException("Unexpected impoundment search results received from VIPS WS. Results > 1 for Notice No: " + noticeNo);
			}
			 
			Long impoundmentId = _resp.getResult().get(0).getImpoundmentId();
				
			// Continue cascade to fetch impoundment object for impoundment id. 
			ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse _resp2 = digitalformsApiService.getImpoundment(correlationId, impoundmentId);
				
			// Depending on the result code from VIPS, we set the response Entity accordingly. 
			if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
				
				// Populate response object from VIPS response object (This removes the VIPS WS resultCd, resultMsg, etc.) 
				resp.setResult(_resp2.getResult());
				
			} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
				logger.error("VIPS " + _resp2.toString());
				throw new DigitalFormsException("Failed to get impoundment for impoundmentId : " + impoundmentId);
			} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
				logger.error("VIPS " + _resp2.toString());
				throw new DigitalFormsException("Internal Java error at VIPS WS. Failed to get impoundment for impoundment Id : " + impoundmentId);
			}	
			
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
			logger.error("VIPS " + _resp.toString());
			throw new DigitalFormsException("Failed to get impoundment for Notice Number : " + noticeNo);
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
			logger.error("VIPS " + _resp.toString());
			throw new DigitalFormsException("Internal Java error at VIPS WS. Failed to get impoundment for Notice Number : " + noticeNo);
		}
		
		return new ResponseEntity<>(resp, HttpStatus.OK);
	}
}
