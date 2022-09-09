package ca.bc.gov.open.digitalformsapi.viirp.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import ca.bc.gov.open.digitalformsapi.viirp.model.GetDFPayloadServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.PostDFPayloadServiceRequest;
import ca.bc.gov.open.digitalformsapi.viirp.model.PostDFPayloadServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.PutDFPayloadServiceRequest;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc(addFilters = false)
public class DFPayloadApiTests {

	// @Autowired
	// private MockMvc mvc;

	@InjectMocks
	private DfPayloadsApiDelegateImpl controller;

	private PostDFPayloadServiceRequest goodPOSTRequest;
	private PutDFPayloadServiceRequest goodPUTRequest;
	private PostDFPayloadServiceRequest badPOSTRequest;

	@BeforeEach
	public void init() {

		MockitoAnnotations.openMocks(this);

		// TODO - mock payload response for ORDS api type.
		goodPOSTRequest = new PostDFPayloadServiceRequest();
		goodPOSTRequest.setActiveYN(true);
		goodPOSTRequest.setProcessedYN(false);
		goodPOSTRequest.setNoticeNo(DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER);
		goodPOSTRequest.setNoticeTypeCd(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);

		Map<String, String> gPayload = new LinkedHashMap<String, String>();
		gPayload.put("car", "Buick");
		gPayload.put("name", "John");
		gPayload.put("noticeType", "IRP");
		gPayload.put("age", "30");

		goodPOSTRequest.setPayload(gPayload);
		
		goodPUTRequest = new PutDFPayloadServiceRequest();
		goodPUTRequest.setActiveYN(true);
		goodPUTRequest.setProcessedYN(false);

		Map<String, String> gPPayload = new LinkedHashMap<String, String>();
		gPayload.put("car", "Toyota");
		gPayload.put("name", "Ted");
		gPayload.put("noticeType", "UL");
		gPayload.put("age", "40");
		
		goodPUTRequest.setPayload(gPPayload);

		badPOSTRequest = new PostDFPayloadServiceRequest();
		badPOSTRequest.setActiveYN(true);
		badPOSTRequest.setProcessedYN(false);
		badPOSTRequest.setNoticeNo(null);
		badPOSTRequest.setNoticeTypeCd(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);

		badPOSTRequest.setPayload(null); // this is a required field. Leaving it null should result in a 500 and validation error msg.

	}

	@DisplayName("GET, Retrieve success - DFPayload Delegate")
	@Test
	public void testDFPayloadApiGETSuccess() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;

		// Create successful GET DF Payload call and validate response
		ResponseEntity<GetDFPayloadServiceResponse> controllerResponse = controller
				.dfpayloadsNoticeNoCorrelationIdGet(noticeNo, correlationId);
		GetDFPayloadServiceResponse result = controllerResponse.getBody();
		Assertions.assertEquals(DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER, result.getNoticeNo());

		// TODO Further testing here to validate the payload is as expected.
		// This will have to be mocked and validated but how to get payload details is
		// below.
		JSONObject payload = (JSONObject) result.getPayload();
		System.out.println(payload.get("noticeType"));
		Assertions.assertEquals(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE, result.getNoticeType());

	}

	// TODO - need further testing for all possible GET ORDS response codes.

	@DisplayName("POST, Create success - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPOSTSuccess() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;

		// Create successful GET DF Payload call and validate response
		ResponseEntity<PostDFPayloadServiceResponse> controllerResponse = controller
				.dfpayloadsNoticeNoCorrelationIdPost(noticeNo, correlationId, goodPOSTRequest);
		PostDFPayloadServiceResponse result = controllerResponse.getBody();
		Assertions.assertEquals(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG, result.getStatusMessage());

	}

	// TODO - need further testing for all possible POST ORDS response codes and
	// perform some simple validation tests.

	@DisplayName("PUT, Update success - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPUTTSuccess() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;

		// Create successful GET DF Payload call and validate response
		ResponseEntity<PostDFPayloadServiceResponse> controllerResponse = controller
				.dfpayloadsNoticeNoCorrelationIdPut(noticeNo, correlationId, goodPUTRequest);
		PostDFPayloadServiceResponse result = controllerResponse.getBody();
		Assertions.assertEquals(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG, result.getStatusMessage());

	}

	// TODO - need further testing for all possible PUT ORDS response codes and
	// perform some simple validation tests.

}
