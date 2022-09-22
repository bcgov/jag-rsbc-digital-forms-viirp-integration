package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc(addFilters = false)
@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class CodeTablesApiControllerTests {
	
	@Autowired
	private MockMvc mvc;
	
	@Mock
    private VipsRestService service;

    @InjectMocks
    private CodeTablesApiDelegateImpl controller;
    
    @Autowired
    ConfigProperties properties;
	
	@Test
    public void testCodeTablesApiGetSuccess() {
        
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
		
		GetCodetablesServiceResponse response = new GetCodetablesServiceResponse();
		response.setRespMsg("Success");

        Mockito.when(service.getCodeTableValues(correlationId)).thenReturn(response);
        ResponseEntity<GetCodetablesServiceResponse> controllerResponse = controller.codetablesCorrelationIdGet(correlationId);
        GetCodetablesServiceResponse result = controllerResponse.getBody();
        Mockito.verify(service).getCodeTableValues(correlationId);
        Assert.assertEquals("Success", result.getRespMsg());
    }
	
	@Test
	public void testCodeTablesApiGetError500WithNoBasicAuth() throws Exception {
		
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
		
	    mvc.perform(get("/codetables/{correlationId}", correlationId)
            		.contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is5xxServerError())
	    	.andExpect(content().contentType("application/json"))
	    	.andExpect(result -> assertTrue(result.getResolvedException() instanceof DigitalFormsException))
	    	.andExpect(jsonPath("$.status_message").value("VIPS API Service failed to respond, after max attempts of: " + properties.getVipsRestApiRetryCount()));
	}
}
