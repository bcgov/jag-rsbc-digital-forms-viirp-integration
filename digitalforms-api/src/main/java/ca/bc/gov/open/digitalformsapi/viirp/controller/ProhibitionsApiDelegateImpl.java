package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.ProhibitionsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@Service
public class ProhibitionsApiDelegateImpl implements ProhibitionsApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(ProhibitionsApiDelegateImpl.class);
	
	@Autowired
	private VipsRestService digitalformsApiService;
	
	@Override
	public ResponseEntity<CreateProhibitionServiceResponse> prohibitionsCorrelationIdPost(String correlationId,
	        CreateProhibition createProhibition) {
		
		logger.info("Heard a call to the endpoint 'prohibitionsCorrelationIdPost'");
		
		CreateProhibitionServiceResponse resp = new CreateProhibitionServiceResponse();
		
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse _resp = digitalformsApiService.createProhibition(correlationId, createProhibition);
		
		// Depending on the result code from VIPS, we set the response Entity accordingly. 
		if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			resp.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
			logger.error("VIPS " + _resp.toString());
			throw new DigitalFormsException("Failed to created prohibition for Notice Number : " + createProhibition.getVipsProhibitionCreate().getProhibitionNoticeNo());
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
			logger.error("VIPS " + _resp.toString());
			throw new DigitalFormsException("Internal Java error at VIPS WS. Failed to created prohibition for Notice Number : " + createProhibition.getVipsProhibitionCreate().getProhibitionNoticeNo());
		}
		
		return new ResponseEntity<>(resp, HttpStatus.OK);
		
	}
	
	@Override
	public ResponseEntity<GetProhibitionServiceResponse> prohibitionsNoticeNoCorrelationIdGet(String noticeNo,
	        String correlationId) {
		
		logger.info("Heard a call to the endpoint 'prohibitionsNoticeNoCorrelationIdGet' with noticeNo " + noticeNo);
		
		GetProhibitionServiceResponse resp = new GetProhibitionServiceResponse();
		
		// Start cascade by fetching prohibitionId for notice number. 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse _resp = digitalformsApiService.searchProhibition(correlationId, noticeNo);
		
		if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
		
			if (null == _resp.getResult() || _resp.getResult().isEmpty()) {
				throw new ResourceNotFoundException("Not Found"); 
			} 
			
			// This shouldn't happen - but if it does, we're ready. 
			if (_resp.getResult().size() > 1) {
					throw new DigitalFormsException("Unexpected prohibition search results received from VIPS WS. Results > 1 for Notice No: " + noticeNo);
			}
			 
			Long prohibitionId = _resp.getResult().get(0).getProhibitionId();
				
			// Continue cascade to fetch prohibition object for prohibition id. 
			ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse _resp2 = digitalformsApiService.getProhibition(correlationId, prohibitionId);
			
			// Depending on the result code from VIPS, we set the response Entity accordingly. 
			if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
				
				// Make sure response has the expected prohibition detail. If this fails we have a bigger problem in VIPS
				// as the prior VIPS search said that there was an prohibition for the id returned above. 
				if (null != _resp2.getResult()) {
				
					// Populate response object from VIPS response object (This removes the VIPS WS resultCd, resultMsg, etc.) 
					resp.setResult(_resp2.getResult());
					
				} else {
					throw new ResourceNotFoundException("No prohibition returned when a prior search indicated there was an prohibition linked to prohibition id  : " + prohibitionId);
				}
				
			} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
				logger.error("VIPS " + _resp2.toString());
				throw new DigitalFormsException("Failed to get prohibition for prohibitionId : " + prohibitionId);
			} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
				logger.error("VIPS " + _resp2.toString());
				throw new DigitalFormsException("Internal Java error at VIPS WS. Failed to get prohibition for prohibition Id : " + prohibitionId);
			}	
			
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
			logger.error("VIPS " + _resp.toString());
			throw new DigitalFormsException("Failed to get prohibition for Notice Number : " + noticeNo);
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
			logger.error("VIPS " + _resp.toString());
			throw new DigitalFormsException("Internal Java error at VIPS WS. Failed to get prohibition for Notice Number : " + noticeNo);
		}
		
		
		return new ResponseEntity<>(resp, HttpStatus.OK);
	}
}
