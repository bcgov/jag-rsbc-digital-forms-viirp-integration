package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
	
	@Mock 
	private DfPayloadService dfPayloadService;  

	@InjectMocks
	private DfPayloadsApiDelegateImpl controller;

	private ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.GetDFPayloadServiceResponse goodDFORDSGETPayloadResponse; 
	private ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse goodDFORDSPOSTPayloadResponse;

	private PostDFPayloadServiceRequest goodPOSTRequest;
	private PutDFPayloadServiceRequest goodPUTRequest;
	private PostDFPayloadServiceRequest badPOSTRequest;

	private String sPayload; 
	
	private ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse responseFromOrds;

	@BeforeEach
	public void init() {

		MockitoAnnotations.openMocks(this);
		
		sPayload = "{\"name\":\"John\",\"noticeType\":\"IRP\",\"car\":\"Buick\",\"age\":35}";
		
		goodDFORDSGETPayloadResponse = new ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.GetDFPayloadServiceResponse();
		goodDFORDSGETPayloadResponse.setActiveYN("Y");
		goodDFORDSGETPayloadResponse.setNoticeType(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);
		goodDFORDSGETPayloadResponse.setPayload(sPayload);
		goodDFORDSGETPayloadResponse.setProcessedYN("Y");
		
		goodDFORDSPOSTPayloadResponse = new ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse();
		goodDFORDSPOSTPayloadResponse.setStatusMessage("Success");
		
		goodPOSTRequest = new PostDFPayloadServiceRequest();
		goodPOSTRequest.setActiveYN(true);
		goodPOSTRequest.setProcessedYN(false);
		goodPOSTRequest.setNoticeTypeCd(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);
		goodPOSTRequest.setPayload(sPayload);
		
		goodPUTRequest = new PutDFPayloadServiceRequest();
		goodPUTRequest.setNoticeTypeCd(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);
		goodPUTRequest.setActiveYN(true);
		goodPUTRequest.setProcessedYN(false);
		goodPUTRequest.setPayload(sPayload);

		badPOSTRequest = new PostDFPayloadServiceRequest();
		badPOSTRequest.setActiveYN(true);
		badPOSTRequest.setProcessedYN(false);
		badPOSTRequest.setNoticeTypeCd(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);

		badPOSTRequest.setPayload(null); // this is a required field. Leaving it null should result in a 500 and validation error msg.
		
		responseFromOrds = new ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse();
	}

	@DisplayName("GET, Retrieve Payload success - DFPayload Delegate")
	@Test
	public void testDFPayloadApiGETSuccess() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.getDFPayload(any(), any())).thenReturn(goodDFORDSGETPayloadResponse);

		// Create successful GET DF Payload call and validate response
		ResponseEntity<GetDFPayloadServiceResponse> controllerResponse = controller.dfpayloadsNoticeNoCorrelationIdGet(noticeNo, correlationId);
		GetDFPayloadServiceResponse result = controllerResponse.getBody();
		
		Assertions.assertEquals(sPayload, result.getPayload());
		Assertions.assertEquals(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE, result.getNoticeType());
		Assertions.assertEquals(true, result.getProcessed()); //
		Assertions.assertEquals(true, result.getActive());
		
	}

	// TODO - need further testing for all possible GET ORDS error response codes (401, 404, and 500) 

	
	@DisplayName("POST, Create Payload success - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPOSTSuccess() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.postDFPayload(any(), any())).thenReturn(goodDFORDSPOSTPayloadResponse);
		
		// Create successful POST DF Payload call and validate response
		ResponseEntity<PostDFPayloadServiceResponse> controllerResponse = controller.dfpayloadsNoticeNoCorrelationIdPost(noticeNo, correlationId, goodPOSTRequest);
		PostDFPayloadServiceResponse result = controllerResponse.getBody();

		Assertions.assertEquals("Success", result.getStatusMessage());

	}

	// TODO - need further testing for all possible POST ORDS error response codes (401, and 500) 

	@DisplayName("PUT, Update Payload success - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPUTSuccess() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.putDFPayload(any(), any(), any())).thenReturn(goodDFORDSPOSTPayloadResponse);

		// Create successful POST DF Payload call and validate response
		ResponseEntity<PostDFPayloadServiceResponse> controllerResponse = controller.dfpayloadsNoticeNoCorrelationIdPut(noticeNo, correlationId, goodPUTRequest);
		PostDFPayloadServiceResponse result = controllerResponse.getBody();

		Assertions.assertEquals("Success", result.getStatusMessage());

	}

	// TODO - need further testing for all possible PUT ORDS error response codes (401, 404, and 500) 
	
	
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

	// TODO - need further testing for all possible DELETE ORDS error response codes (401, 404, and 500)
	
}
