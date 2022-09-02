package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
	public ResponseEntity<GetDFPayloadServiceResponse> dfpayloadsNoticeNoCorrelationIdGet(String noticeNo,
	        String correlationId) {	
		
		logger.info("Heard a call to the endpoint 'dfPayloadsNoticeNoCorrelationIdGet' with noticeNo " + noticeNo);
		
		GetDFPayloadServiceResponse resp = new GetDFPayloadServiceResponse();
		
		String examplePayload = "{\"name\":\"John\", \"age\":30, \"car\":\"Buick\", \"noticeType\":\"IRP\"}";
		JSONParser parser = new JSONParser();  
		JSONObject json = null;
		try {
			json = (JSONObject) parser.parse(examplePayload);
		} catch (ParseException e) {
			e.printStackTrace();
		} 
		
		// TODO - remove this when ORDS connected - for initial testing only. 
		resp.setActive(true);
		resp.setNoticeNo("90902601");
		resp.setNoticeType("IRP");
		resp.setPayload(json); 
		resp.setProcessed(false);
		
		// TODO - Call Digital Forms ORDS client to GET a payload (once complete). 
		// load response object dependent on the response (and possible response codes). 
		
		return new ResponseEntity<>(resp, HttpStatus.OK);
	}
}
