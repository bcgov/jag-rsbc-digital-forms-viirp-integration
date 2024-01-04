package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.UnauthorizedException;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetDFPayloadServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.PostDFPayloadServiceRequest;
import ca.bc.gov.open.digitalformsapi.viirp.model.PostDFPayloadServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.PutDFPayloadServiceRequest;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.handler.ApiException;
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
	
	private ApiException unauthorizedApiException;
	
	private ApiException notFoundApiException;
	
	private ApiException internalServerErrorApiException;

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
		goodPOSTRequest.setActive(true);
		goodPOSTRequest.setProcessed(false);
		goodPOSTRequest.setNoticeTypeCd(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);
		goodPOSTRequest.setPayload(sPayload);
		
		goodPUTRequest = new PutDFPayloadServiceRequest();
		goodPUTRequest.setNoticeTypeCd(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);
		goodPUTRequest.setActive(true);
		goodPUTRequest.setProcessed(false);
		goodPUTRequest.setPayload(sPayload);

		badPOSTRequest = new PostDFPayloadServiceRequest();
		badPOSTRequest.setActive(true);
		badPOSTRequest.setProcessed(false);
		badPOSTRequest.setNoticeTypeCd(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE);

		badPOSTRequest.setPayload(null); // this is a required field. Leaving it null should result in a 500 and validation error msg.
		
		responseFromOrds = new ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse();
		
		// An ApiException which has 401 status code for testing api call responses that are expected to throw unauthorized ApiException
		unauthorizedApiException = new ApiException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized exception");
		
		// An ApiException which has 404 status code for testing api call responses that are expected to throw not found ApiException
		notFoundApiException = new ApiException(HttpStatus.NOT_FOUND.value(), "Not Found exception");
		
		// An ApiException which has 500 status code for testing api call responses that are expected to throw internal server error ApiException
		internalServerErrorApiException = new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Not Found exception");
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
		Assertions.assertEquals(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE, result.getNoticeTypeCd());
		Assertions.assertEquals(true, result.getProcessed()); //
		Assertions.assertEquals(true, result.getActive());
		
	}
	
	@DisplayName("GET, Retrieve Payload, expect 401 Unauthorized - DFPayload Delegate")
	@Test
	public void testDFPayloadApiGET401Unauthorized() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.getDFPayload(any(), any())).thenThrow(unauthorizedApiException);
		
		// Create GET DF Payload call and ensure UnauthorizedException type of exception is thrown in this case
		assertThrows(UnauthorizedException.class, () -> {
			controller.dfpayloadsNoticeNoCorrelationIdGet(noticeNo, correlationId);
		});
	}
	
	@DisplayName("GET, Retrieve Payload, expect 404 Not Found - DFPayload Delegate")
	@Test
	public void testDFPayloadApiGET404NotFound() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.getDFPayload(any(), any())).thenThrow(notFoundApiException);
		
		// Create GET DF Payload call and ensure ResourceNotFoundException type of exception is thrown in this case
		Assertions.assertThrows(ResourceNotFoundException.class, () -> {
			controller.dfpayloadsNoticeNoCorrelationIdGet(noticeNo, correlationId);
		});
	}
	
	@DisplayName("GET, Retrieve Payload, expect 500 Internal Server Error - DFPayload Delegate")
	@Test
	public void testDFPayloadApiGET500InternalServerError() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.getDFPayload(any(), any())).thenThrow(internalServerErrorApiException);
		
		// Create GET DF Payload call and ensure DigitalFormsException type of exception is thrown in this case
		Assertions.assertThrows(DigitalFormsException.class, () -> {
			controller.dfpayloadsNoticeNoCorrelationIdGet(noticeNo, correlationId);
		});
	}
	
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
	
	@DisplayName("POST, Create Payload, expect 401 Unauthorized - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPOST401Unauthorized() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.postDFPayload(any(), any())).thenThrow(unauthorizedApiException);
		
		// Create POST DF Payload call and ensure UnauthorizedException type of exception is thrown in this case
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			controller.dfpayloadsNoticeNoCorrelationIdPost(noticeNo, correlationId, goodPOSTRequest);
		});
	}
	
	@DisplayName("POST, Create Payload, expect 500 Internal Server Error - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPOST500InternalServerError() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.postDFPayload(any(), any())).thenThrow(internalServerErrorApiException);
		
		// Create POST DF Payload call and ensure DigitalFormsException type of exception is thrown in this case
		Assertions.assertThrows(DigitalFormsException.class, () -> {
			controller.dfpayloadsNoticeNoCorrelationIdPost(noticeNo, correlationId, goodPOSTRequest);
		});
	}

	@DisplayName("PUT, Update Payload success - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPUTSuccess() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.putDFPayload(any(), any(), any())).thenReturn(goodDFORDSPOSTPayloadResponse);

		// Create successful PUT DF Payload call and validate response
		ResponseEntity<PostDFPayloadServiceResponse> controllerResponse = controller.dfpayloadsNoticeNoCorrelationIdPut(noticeNo, correlationId, goodPUTRequest);
		PostDFPayloadServiceResponse result = controllerResponse.getBody();

		Assertions.assertEquals("Success", result.getStatusMessage());

	}
	
	@DisplayName("PUT, Update Payload, expect 401 Unauthorized - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPUT401Unauthorized() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.putDFPayload(any(), any(), any())).thenThrow(unauthorizedApiException);
		
		// Create PUT DF Payload call and ensure UnauthorizedException type of exception is thrown in this case
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			controller.dfpayloadsNoticeNoCorrelationIdPut(noticeNo, correlationId, goodPUTRequest);
		});
	}
	
	@DisplayName("PUT, Update Payload, expect 404 Not Found - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPUT404NotFound() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.putDFPayload(any(), any(), any())).thenThrow(notFoundApiException);
		
		// Create PUT DF Payload call and ensure ResourceNotFoundException type of exception is thrown in this case
		Assertions.assertThrows(ResourceNotFoundException.class, () -> {
			controller.dfpayloadsNoticeNoCorrelationIdPut(noticeNo, correlationId, goodPUTRequest);
		});
	}
	
	@DisplayName("PUT, Update Payload, expect 500 Internal Server Error - DFPayload Delegate")
	@Test
	public void testDFPayloadApiPUT500InternalServerError() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.putDFPayload(any(), any(), any())).thenThrow(internalServerErrorApiException);
		
		// Create PUT DF Payload call and ensure DigitalFormsException type of exception is thrown in this case
		Assertions.assertThrows(DigitalFormsException.class, () -> {
			controller.dfpayloadsNoticeNoCorrelationIdPut(noticeNo, correlationId, goodPUTRequest);
		});
	}
	
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
	
	@DisplayName("DEL, Remove Payload, expect 401 Unauthorized - DFPayload Delegate")
	@Test
	public void testDFPayloadApiDELETE401Unauthorized() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.deleteDFPayload(any(), any())).thenThrow(unauthorizedApiException);
		
		// Create DELETE DF Payload call and ensure UnauthorizedException type of exception is thrown in this case
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			controller.dfpayloadsNoticeNoCorrelationIdDelete(noticeNo, correlationId);
		});
	}
	
	@DisplayName("DEL, Remove Payload, expect 404 Not Found - DFPayload Delegate")
	@Test
	public void testDFPayloadApiDELETE404NotFound() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.deleteDFPayload(any(), any())).thenThrow(notFoundApiException);
		
		// Create DELETE DF Payload call and ensure ResourceNotFoundException type of exception is thrown in this case
		Assertions.assertThrows(ResourceNotFoundException.class, () -> {
			controller.dfpayloadsNoticeNoCorrelationIdDelete(noticeNo, correlationId);
		});
	}
	
	@DisplayName("DEL, Remove Payload, expect 500 Internal Server Error - DFPayload Delegate")
	@Test
	public void testDFPayloadApiDELETE500InternalServerError() throws Exception {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		when(dfPayloadService.deleteDFPayload(any(), any())).thenThrow(internalServerErrorApiException);
		
		// Create DELETE DF Payload call and ensure DigitalFormsException type of exception is thrown in this case
		Assertions.assertThrows(DigitalFormsException.class, () -> {
			controller.dfpayloadsNoticeNoCorrelationIdDelete(noticeNo, correlationId);
		});
	}
}
