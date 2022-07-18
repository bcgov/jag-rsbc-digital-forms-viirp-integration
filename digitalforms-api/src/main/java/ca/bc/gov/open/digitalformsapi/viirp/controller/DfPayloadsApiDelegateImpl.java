package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.DfPayloadsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetDFPayloadServiceResponse;

@Service
public class DfPayloadsApiDelegateImpl implements DfPayloadsApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(DfPayloadsApiDelegateImpl.class);
	
	@Override
	public ResponseEntity<GetDFPayloadServiceResponse> dfpayloadsNoticeNoCorrelationIdGet(Long noticeNo,
	        String correlationId) {
		
		logger.info("Heard a call to the endpoint 'dfDocumentNoticeNoCorrelationIdGe' with noticeNo " + noticeNo);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
