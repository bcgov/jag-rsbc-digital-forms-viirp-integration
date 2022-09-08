package ca.bc.gov.open.digitalformsapi.viirp.controller;

import java.util.LinkedHashMap;

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
import ca.bc.gov.open.digitalformsapi.viirp.model.PostDFPayloadServiceRequest;
import ca.bc.gov.open.digitalformsapi.viirp.model.PostDFPayloadServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.PutDFPayloadServiceRequest;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@Service
public class DfPayloadsApiDelegateImpl implements DfPayloadsApiDelegate {

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
		resp.setNoticeNo(DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER);
		resp.setNoticeType("IRP");
		resp.setPayload(json);
		resp.setProcessed(false);

		// TODO - Call Digital Forms ORDS client to GET a payload.
		// Load response object dependent on the good ORDS response, or
		// Throw appropriate exceptions.

		return new ResponseEntity<>(resp, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<PostDFPayloadServiceResponse> dfpayloadsNoticeNoCorrelationIdPost(String correlationId,
			String noticeNo, PostDFPayloadServiceRequest postDFPayloadServiceRequest) {

		logger.info("Heard a call to the endpoint 'dfpayloadsNoticeNoCorrelationIdPost' with noticeNo " + noticeNo);
		
		@SuppressWarnings("rawtypes")
		LinkedHashMap payload = (LinkedHashMap) postDFPayloadServiceRequest.getPayload();
		
		JSONObject json = new JSONObject(payload);
		
		logger.info("Payload is " + json.toJSONString());
		
		// TODO - need to pass the params to the ORDS call to store the notice no data. 
		// TODO - need to determine the datatype required to store the payload as clob.
		// TODO - need to set response type (or exception) based on ORDS response. 

		PostDFPayloadServiceResponse resp = new PostDFPayloadServiceResponse();
		resp.setStatusMessage("success");

		return new ResponseEntity<>(resp, HttpStatus.NOT_IMPLEMENTED);

	}
	
	@Override
	public ResponseEntity<PostDFPayloadServiceResponse> dfpayloadsNoticeNoCorrelationIdPut(String correlationId,
			String noticeNo, PutDFPayloadServiceRequest putDFPayloadServiceRequest) {
		
		logger.info("Heard a call to the endpoint 'dfpayloadsNoticeNoCorrelationIdPut' with noticeNo " + noticeNo);
		
		@SuppressWarnings("rawtypes")
		LinkedHashMap payload = (LinkedHashMap) putDFPayloadServiceRequest.getPayload();
		
		JSONObject json = new JSONObject(payload);
		
		logger.info("Payload is " + json.toJSONString());
		
		// TODO - need to pass the params to the ORDS call to update notice no data. 
		// TODO - need to determine the datatype required to update the payload as clob.
		// TODO - need to set response type (or exception) based on ORDS response. 

		PostDFPayloadServiceResponse resp = new PostDFPayloadServiceResponse();
		resp.setStatusMessage("success");
		
		return new ResponseEntity<>(resp, HttpStatus.NOT_IMPLEMENTED);

	}
}
