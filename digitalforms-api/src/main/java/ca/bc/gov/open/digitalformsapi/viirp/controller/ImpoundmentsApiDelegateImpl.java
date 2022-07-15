package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.ImpoundmentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetImpoundmentServiceResponse;

@Service
public class ImpoundmentsApiDelegateImpl implements ImpoundmentsApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(ImpoundmentsApiDelegateImpl.class);
	
	@Override
	public ResponseEntity<CreateImpoundmentServiceResponse> impoundmentsCorrelationIdPost(String correlationId,
	        CreateImpoundment createImpoundment) {
		
		logger.info("Heard a call to the endpoint 'impoundmentsCorrelationIdPost'");
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<GetImpoundmentServiceResponse> impoundmentsNoticeNoCorrelationIdGet(String noticeNo,
	        String correlationId) {
		
		logger.info("Heard a call to the endpoint 'impoundmentsImpoundmentIdCorrelationIdGet' with noticeNo " + noticeNo);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
