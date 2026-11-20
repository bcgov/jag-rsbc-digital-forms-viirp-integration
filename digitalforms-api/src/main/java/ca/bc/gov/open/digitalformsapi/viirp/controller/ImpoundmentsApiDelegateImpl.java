package ca.bc.gov.open.digitalformsapi.viirp.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import ca.bc.gov.open.digitalformsapi.viirp.api.ImpoundmentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.BadRequestException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.UpstreamServiceException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundmentResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.Impoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.RegistrationCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.service.NoticeConflictService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@Service
public class ImpoundmentsApiDelegateImpl implements ImpoundmentsApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(ImpoundmentsApiDelegateImpl.class);
	
	@Autowired
	private VipsRestService digitalformsApiService;

	@Autowired
	private NoticeConflictService noticeConflicts;

	@Autowired
	private HttpServletRequest request;

	/**
	 * POST Impoundment (TO VIPS WS) 
	 * 
	 * Possible Response codes:
	 * 	201. created. 
	 * 	401. invalid or missing bearer token
	 * 	502/503/504. VIPS response failure, unavailability or timeout.
	 * 	500. local VI/IRP processing failure.
	 */
	@Override
	public ResponseEntity<CreateImpoundmentResponse> createImpoundment(CreateImpoundment createImpoundment,
	        String xCorrelationID) {
		
		logger.info("Heard a call to the endpoint 'createImpoundment'");
		
		// DF-2933: If a "licence" node is present, ensure the DL is supplied.
		for (int index = 0; index < createImpoundment.getRegistrations().size(); index++) {
			RegistrationCreate reg = createImpoundment.getRegistrations().get(index);
			String field = "registrations[" + index + "]";
			if (reg == null) {
				throw new BadRequestException(field, "registrations must not contain null entries.");
			}
			RegistrationValidation.requireName(reg, field);
			if (null != reg.getLicence()) {
				if (!StringUtils.hasText(reg.getLicence().getDriverLicenceNo())) {
					throw new BadRequestException(field + ".licence.driverLicenceNo",
							DigitalFormsConstants.DIGITALFORMS_VALIDATION_FAILURE_MISSING_DL);
				}
			}
		}
		
		noticeConflicts.checkImpoundment(createImpoundment.getImpoundment().getImpoundmentNoticeNo());

		CreateImpoundmentResponse resp = new CreateImpoundmentResponse();
		
		var _resp = digitalformsApiService.createImpoundment(createImpoundment);
		if (_resp == null) {
			throw UpstreamServiceException.invalidResponse("VIPS returned an empty impoundment create response.");
		}
		
		// Depending on the result code from VIPS, we set the response Entity accordingly. 
		if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD // SUCCESS_MSG
				|| _resp.getRespCd() == DigitalFormsConstants.VIPSWS_SHOW_MANUAL_DIALOG_CD // SUCCESS_MANUAL_DRIVER_UPDATE_MSG
				|| _resp.getRespCd() == DigitalFormsConstants.VIPSWS_INTERNAL_FAILURE_CD) { // SUCCESS_ICBC_NOT_UPDATED_MSG
			// no-op, we can continue to return the impoundmentId from the response.
		}
		else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
			// Preserve the database constraint as the final guard against concurrent creates.
			noticeConflicts.checkImpoundment(createImpoundment.getImpoundment().getImpoundmentNoticeNo());
			logger.error("VIPS error response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
			throw UpstreamServiceException.invalidResponse("Failed to create impoundment for Notice Number : " + createImpoundment.getImpoundment().getImpoundmentNoticeNo());
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
			logger.error("VIPS error response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
			throw UpstreamServiceException.invalidResponse("Internal Java error at VIPS WS. Failed to create impoundment for Notice Number : " + createImpoundment.getImpoundment().getImpoundmentNoticeNo());
		} else {
			logger.error("Unexpected VIPS response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
			throw UpstreamServiceException.invalidResponse("Unexpected VIPS WS response code " + _resp.getRespCd() + ". Failed to create impoundment for Notice Number : " + createImpoundment.getImpoundment().getImpoundmentNoticeNo());
		}
		if (_resp.getImpoundmentId() == null) {
			throw UpstreamServiceException.invalidResponse("VIPS WS did not return an impoundment ID for Notice Number : " + createImpoundment.getImpoundment().getImpoundmentNoticeNo());
		}
		resp.setImpoundmentId(_resp.getImpoundmentId());
		
		return ResponseEntity.created(UriComponentsBuilder.fromPath(request.getContextPath())
				.pathSegment("impoundments", createImpoundment.getImpoundment().getImpoundmentNoticeNo())
				.build().encode().toUri()).body(resp);
		
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
	 * 	401. invalid or missing bearer token
	 *  404. Not found (Initial search call failed to find impoundment for given notice number)
	 * 	502/503/504. VIPS response failure, unavailability or timeout.
	 * 	500. local VI/IRP processing failure.
	 */
	@Override
	public ResponseEntity<Impoundment> getImpoundment(String noticeNo, String xCorrelationID) {
		
		logger.info("Heard a call to the endpoint 'getImpoundment' with noticeNo " + noticeNo);
		
		Impoundment resp;
		
		// Start cascade by fetching impoundmentId for notice number. 
		SearchImpoundmentsServiceResponse _resp = digitalformsApiService.searchImpoundment(noticeNo);
		if (_resp == null) {
			throw UpstreamServiceException.invalidResponse("VIPS returned an empty impoundment search response.");
		}
		
		if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
		
			if (null == _resp.getResult() || _resp.getResult().isEmpty()) {
				throw new ResourceNotFoundException("Not Found"); 
			} 
			
			// This shouldn't happen - but if it does, we're ready. 
			if (_resp.getResult().size() > 1 || _resp.getResult().getFirst() == null
					|| _resp.getResult().getFirst().getImpoundmentId() == null) {
					throw UpstreamServiceException.invalidResponse("Invalid impoundment search results received from VIPS WS for Notice No: " + noticeNo);
			}
			 
			Long impoundmentId = _resp.getResult().get(0).getImpoundmentId();
				
			// Continue cascade to fetch impoundment object for impoundment id. 
			var _resp2 = digitalformsApiService.getImpoundment(impoundmentId);
			if (_resp2 == null) {
				throw UpstreamServiceException.invalidResponse("VIPS returned an empty impoundment response.");
			}
				
			// Depending on the result code from VIPS, we set the response Entity accordingly. 
			if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
				
				// Make sure response has the expected impoundment detail. If this fails we have a bigger problem in VIPS
				// as the prior VIPS search said that there was an impoundment for the id returned above. 
				if (null != _resp2.getResult()) {
				
					resp = _resp2.getResult();
					
				} else {
					throw new ResourceNotFoundException("No impoundment returned when a prior search indicated there was an impoundment linked to impoundment id  : " + impoundmentId);
				}
				
			} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
				logger.error("VIPS error response code: " + _resp2.getRespCd() + ", response msg:  " + _resp2.getRespMsg());
				throw UpstreamServiceException.invalidResponse("Failed to get impoundment for impoundmentId : " + impoundmentId);
			} else if (_resp2.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
				logger.error("VIPS error response code: " + _resp2.getRespCd() + ", response msg:  " + _resp2.getRespMsg());
				throw UpstreamServiceException.invalidResponse("Internal Java error at VIPS WS. Failed to get impoundment for impoundment Id : " + impoundmentId);
			} else {
				throw UpstreamServiceException.invalidResponse("Unexpected VIPS response code: " + _resp2.getRespCd());
			}	
			
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD) {
			logger.error("VIPS error response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
			throw UpstreamServiceException.invalidResponse("Failed to get impoundment for Notice Number : " + noticeNo);
		} else if (_resp.getRespCd() == DigitalFormsConstants.VIPSWS_JAVA_EX) {
			logger.error("VIPS error response code: " + _resp.getRespCd() + ", response msg:  " + _resp.getRespMsg());
			throw UpstreamServiceException.invalidResponse("Internal Java error at VIPS WS. Failed to get impoundment for Notice Number : " + noticeNo);
		} else {
			throw UpstreamServiceException.invalidResponse("Unexpected VIPS response code: " + _resp.getRespCd());
		}
		
		return new ResponseEntity<>(resp, HttpStatus.OK);
	}
}
