package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.ProhibitionsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
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
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
