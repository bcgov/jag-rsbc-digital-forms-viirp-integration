package ca.bc.gov.open.digitalformsapi.viirp.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import ca.bc.gov.open.digitalformsapi.viirp.api.ProhibitionsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.UpstreamServiceException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibitionResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.Prohibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.service.NoticeConflictService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@Service
public class ProhibitionsApiDelegateImpl implements ProhibitionsApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(ProhibitionsApiDelegateImpl.class);
	
	@Autowired
	private VipsRestService digitalformsApiService;

	@Autowired
	private NoticeConflictService noticeConflicts;

	@Autowired
	private HttpServletRequest request;
	
	@Override
	public ResponseEntity<CreateProhibitionResponse> createProhibition(CreateProhibition createProhibition,
	        String xCorrelationID) throws DigitalFormsException {
		
		logger.info("Heard a call to the endpoint 'createProhibition'");

		RegistrationValidation.requireName(createProhibition.getRegistration(), "registration");
		noticeConflicts.checkProhibition(createProhibition.getProhibition().getProhibitionNoticeNo());
		
		CreateProhibitionResponse resp = new CreateProhibitionResponse();
		
		var _resp = digitalformsApiService.createProhibition(createProhibition);
		if (_resp == null) {
			throw UpstreamServiceException.invalidResponse("VIPS returned an empty prohibition create response.");
		}
		
		// Depending on the result code from VIPS, we set the response Entity accordingly. 
		if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_SHOW_MANUAL_DIALOG_CD) {
			logger.warn("VIPS created prohibition with warning. Response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_INTERNAL_FAILURE_CD) {
			logger.warn("VIPS created prohibition with upstream post-processing failure. Response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
			// A concurrent create can win after the availability check; only a confirmed duplicate is a 409.
			noticeConflicts.checkProhibition(createProhibition.getProhibition().getProhibitionNoticeNo());
			logger.error("VIPS error response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
			throw UpstreamServiceException.invalidResponse("Failed to create prohibition for Notice Number : " + createProhibition.getProhibition().getProhibitionNoticeNo());
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
			logger.error("VIPS error response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
			throw UpstreamServiceException.invalidResponse("Internal Java error at VIPS WS. Failed to create prohibition for Notice Number : " + createProhibition.getProhibition().getProhibitionNoticeNo());
		} else if (_resp.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			logger.error("Unexpected VIPS response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
			throw UpstreamServiceException.invalidResponse("Unexpected VIPS WS response code " + _resp.getRespCd() + ". Failed to create prohibition for Notice Number : " + createProhibition.getProhibition().getProhibitionNoticeNo());
		}
		if (_resp.getProhibitionId() == null) {
			throw UpstreamServiceException.invalidResponse("VIPS WS did not return a prohibition ID for Notice Number : " + createProhibition.getProhibition().getProhibitionNoticeNo());
		}
		resp.setProhibitionId(_resp.getProhibitionId());
		
		return ResponseEntity.created(UriComponentsBuilder.fromPath(request.getContextPath())
				.pathSegment("prohibitions", createProhibition.getProhibition().getProhibitionNoticeNo())
				.build().encode().toUri()).body(resp);
		
	}
	
	@Override
	public ResponseEntity<Prohibition> getProhibition(String noticeNo, String xCorrelationID) {
		
		logger.info("Heard a call to the endpoint 'getProhibition' with noticeNo " + noticeNo);
		
		Prohibition resp;
		
		// Start cascade by fetching prohibitionId for notice number. 
		SearchProhibitionsServiceResponse _resp = digitalformsApiService.searchProhibition(noticeNo);
		if (_resp == null) {
			throw UpstreamServiceException.invalidResponse("VIPS returned an empty prohibition search response.");
		}
		
		if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
		
			if (null == _resp.getResult() || _resp.getResult().isEmpty()) {
				throw new ResourceNotFoundException("Not Found"); 
			} 
			
			// This shouldn't happen - but if it does, we're ready. 
			if (_resp.getResult().size() > 1 || _resp.getResult().getFirst() == null
					|| _resp.getResult().getFirst().getProhibitionId() == null) {
					throw UpstreamServiceException.invalidResponse("Invalid prohibition search results received from VIPS WS for Notice No: " + noticeNo);
			}
			 
			Long prohibitionId = _resp.getResult().get(0).getProhibitionId();
				
			// Continue cascade to fetch prohibition object for prohibition id. 
			var _resp2 = digitalformsApiService.getProhibition(prohibitionId);
			if (_resp2 == null) {
				throw UpstreamServiceException.invalidResponse("VIPS returned an empty prohibition response.");
			}
			
			// Depending on the result code from VIPS, we set the response Entity accordingly. 
			if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
				
				// Make sure response has the expected prohibition detail. If this fails we have a bigger problem in VIPS
				// as the prior VIPS search said that there was an prohibition for the id returned above. 
				if (null != _resp2.getResult()) {
				
					resp = _resp2.getResult();
					
				} else {
					throw new ResourceNotFoundException("No prohibition returned when a prior search indicated there was an prohibition linked to prohibition id  : " + prohibitionId);
				}
				
			} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
				logger.error("VIPS error response code: " + _resp2.getRespCd() + ", response msg:  " + _resp2.getRespMsg());
				throw UpstreamServiceException.invalidResponse("Failed to get prohibition for prohibitionId : " + prohibitionId);
			} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
				logger.error("VIPS error response code: " + _resp2.getRespCd() + ", response msg:  " + _resp2.getRespMsg());
				throw UpstreamServiceException.invalidResponse("Internal Java error at VIPS WS. Failed to get prohibition for prohibition Id : " + prohibitionId);
			} else {
				throw UpstreamServiceException.invalidResponse("Unexpected VIPS response code: " + _resp2.getRespCd());
			}	
			
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
			logger.error("VIPS error response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
			throw UpstreamServiceException.invalidResponse("Failed to get prohibition for Notice Number : " + noticeNo);
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
			logger.error("VIPS error response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
			throw UpstreamServiceException.invalidResponse("Internal Java error at VIPS WS. Failed to get prohibition for Notice Number : " + noticeNo);
		} else {
			throw UpstreamServiceException.invalidResponse("Unexpected VIPS response code: " + _resp.getRespCd());
		}
		
		
		return new ResponseEntity<>(resp, HttpStatus.OK);
	}
}
