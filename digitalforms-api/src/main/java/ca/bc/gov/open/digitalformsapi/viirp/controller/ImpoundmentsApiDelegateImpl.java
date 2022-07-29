package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.ImpoundmentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@Service
public class ImpoundmentsApiDelegateImpl implements ImpoundmentsApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(ImpoundmentsApiDelegateImpl.class);
	
	@Autowired
	private VipsRestService digitalformsApiService;
	
	/**
	 * What http status code can this respond with??
	 * 
	 * 201. created. 
	 * 400. bad request
	 * 401. unauth (digital forms basic auth)
	 * 500. from the ssg. 
	 * 
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

	@Override
	public ResponseEntity<GetImpoundmentServiceResponse> impoundmentsNoticeNoCorrelationIdGet(String noticeNo,
	        String correlationId) {
		
		logger.info("Heard a call to the endpoint 'impoundmentsImpoundmentIdCorrelationIdGet' with noticeNo " + noticeNo);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
