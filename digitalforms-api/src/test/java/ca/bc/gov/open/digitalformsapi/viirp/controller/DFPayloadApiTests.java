package ca.bc.gov.open.digitalformsapi.viirp.controller;

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
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc(addFilters = false)
public class DFPayloadApiTests {

	//@Autowired
	//private MockMvc mvc;

	@InjectMocks
	private DfPayloadsApiDelegateImpl controller;
	
	// Mock private ORDS type to fetch the payload with. 
	// mock it with good and bad response types. 
	
	@BeforeEach
	public void init() {

		MockitoAnnotations.openMocks(this);
		
		//TODO - mock payload response for ORDS api type. 

	}
	
	
    @DisplayName("GET, Fetch Success - DFPayload Delegate")  
    @Test
	public void testDFPayloadApiGETSuccess() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	
    	// Create successful search notice number call with impoundmentId and validate response 
        ResponseEntity<GetDFPayloadServiceResponse> controllerResponse = controller.dfpayloadsNoticeNoCorrelationIdGet(noticeNo, correlationId);
        GetDFPayloadServiceResponse result = controllerResponse.getBody();
        Assertions.assertEquals(DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER, result.getNoticeNo());
        
        // TODO Further testing here to validate the payload is as expected. 
        // This will have to be mocked and validated but how to get payload details is below. 
        JSONObject payload = (JSONObject) result.getPayload();
        System.out.println(payload.get("noticeType"));
        Assertions.assertEquals(DigitalFormsConstants.UNIT_TEST_NOTICE_TYPE, result.getNoticeType());
        
    }
    
    //TODO - need further testing for all possible ORDS response codes. 
    
}
