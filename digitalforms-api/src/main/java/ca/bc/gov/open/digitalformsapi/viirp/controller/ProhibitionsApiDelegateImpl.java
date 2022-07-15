package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.ProhibitionsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetProhibitionServiceResponse;

@Service
public class ProhibitionsApiDelegateImpl implements ProhibitionsApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(ProhibitionsApiDelegateImpl.class);
	
	@Override
	public ResponseEntity<CreateProhibitionServiceResponse> prohibitionsCorrelationIdPost(String correlationId,
	        CreateProhibition createProhibition) {
		
		logger.info("Heard a call to the endpoint 'prohibitionsCorrelationIdPost'");
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<GetProhibitionServiceResponse> prohibitionsNoticeNoCorrelationIdGet(String noticeNo,
	        String correlationId) {
		
		logger.info("Heard a call to the endpoint 'prohibitionsNoticeNoCorrelationIdGet' with noticeNo " + noticeNo);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
