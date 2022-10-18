package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.payload.DfPayloadService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc(addFilters = false)
public class DFPayloadApiTests {

	// @Autowired
	// private MockMvc mvc;
	
	@Mock 
	private DfPayloadService dfPayloadService;  

	@InjectMocks
	private DfPayloadsApiDelegateImpl controller;

	private ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.GetDFPayloadServiceResponse goodDFORDSPayloadResponse; 

	private PostDFPayloadServiceRequest goodPOSTRequest;
	private PutDFPayloadServiceRequest goodPUTRequest;
	private PostDFPayloadServiceRequest badPOSTRequest;

	Map<String, String> gPayload;
	private ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse responseFromOrds;

	@BeforeEach
	public void init() {

		MockitoAnnotations.openMocks(this);
		
		gPayload = new LinkedHashMap<String, String>();
		gPayload.put("car", "Buick");
		gPayload.put("name", "John");
		gPayload.put("noticeType", "IRP");
		gPayload.put("age", "30");
		
		goodDFORDSPayloadResponse = new ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.GetDFPayloadServiceResponse();
		goodDFORDSPayloadResponse.setActiveYN("Y");
		goodDFORDSPayloadResponse.setNoticeType(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);
		goodDFORDSPayloadResponse.setPayload(gPayload);
		goodDFORDSPayloadResponse.setProcessedYN("Y");
		
		// TODO - mock payload response for ORDS api type.
		goodPOSTRequest = new PostDFPayloadServiceRequest();
		goodPOSTRequest.setActiveYN(true);
		goodPOSTRequest.setProcessedYN(false);
		goodPOSTRequest.setNoticeNo(DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER);
		goodPOSTRequest.setNoticeTypeCd(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);
		goodPOSTRequest.setPayload(gPayload);
		
		goodPUTRequest = new PutDFPayloadServiceRequest();
		goodPUTRequest.setActiveYN(true);
		goodPUTRequest.setProcessedYN(false);
		goodPUTRequest.setPayload(gPayload);

		badPOSTRequest = new PostDFPayloadServiceRequest();
		badPOSTRequest.setActiveYN(true);
		badPOSTRequest.setProcessedYN(false);
		badPOSTRequest.setNoticeNo(null);
		badPOSTRequest.setNoticeTypeCd(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);

		badPOSTRequest.setPayload(null); // this is a required field. Leaving it null should result in a 500 and validation error msg.
		
		responseFromOrds = new ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse();
	}

	@DisplayName("GET, Retrieve Payload success - DFPayload Delegate")
	@Test
	public void testDFPayloadApiGETSuccess() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.getDFPayload(any(), any())).thenReturn(goodDFORDSPayloadResponse);

		// Create successful GET DF Payload call and validate response
		ResponseEntity<GetDFPayloadServiceResponse> controllerResponse = controller.dfpayloadsNoticeNoCorrelationIdGet(noticeNo, correlationId);
		GetDFPayloadServiceResponse result = controllerResponse.getBody();
		
		Assertions.assertEquals(gPayload, result.getPayload());
		Assertions.assertEquals(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE, result.getNoticeType());
		Assertions.assertEquals(true, result.getProcessed()); //
		Assertions.assertEquals(true, result.getActive());
		
	}

	// TODO - need further testing for all possible GET ORDS response codes once we have them. 

	@DisplayName("POST, Create Payload success - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPOSTSuccess() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;

		// Create successful POST DF Payload call and validate response
		ResponseEntity<PostDFPayloadServiceResponse> controllerResponse = controller
				.dfpayloadsNoticeNoCorrelationIdPost(noticeNo, correlationId, goodPOSTRequest);
		PostDFPayloadServiceResponse result = controllerResponse.getBody();
		Assertions.assertEquals(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG, result.getStatusMessage());

	}

	// TODO - need further testing for all possible POST ORDS response codes and
	// perform some simple validation tests.

	@DisplayName("PUT, Update Payload success - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPUTSuccess() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;

		// Create successful UPDATE DF Payload call and validate response
		ResponseEntity<PostDFPayloadServiceResponse> controllerResponse = controller
				.dfpayloadsNoticeNoCorrelationIdPut(noticeNo, correlationId, goodPUTRequest);
		PostDFPayloadServiceResponse result = controllerResponse.getBody();
		Assertions.assertEquals(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG, result.getStatusMessage());

	}

	// TODO - need further testing for all possible PUT ORDS response codes and
	// perform some simple validation tests.
	
	
	@DisplayName("DEL, Remove Payload success - DFPayload Delegate")
	@Test
	public void testDFPayloadApiDELETESuccess() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		// Mock underlying DELETE DF Payload response in good case 
		responseFromOrds.setStatusMessage(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
        when(dfPayloadService.deleteDFPayload(any(), any())).thenReturn(responseFromOrds);

		// Create successful DELETE DF Payload and validate response
		ResponseEntity<PostDFPayloadServiceResponse> controllerResponse = controller
				.dfpayloadsNoticeNoCorrelationIdDelete(noticeNo, correlationId);
		PostDFPayloadServiceResponse result = controllerResponse.getBody();
		Assertions.assertEquals(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG, result.getStatusMessage());
	}

	// TODO - need further testing for all possible PUT ORDS response codes and
	// perform some simple validation tests.
	
}
