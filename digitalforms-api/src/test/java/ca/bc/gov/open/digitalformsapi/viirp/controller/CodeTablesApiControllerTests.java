package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import java.io.IOException;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
//import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc(addFilters = false)
//@RunWith(MockitoJUnitRunner.class)
class CodeTablesApiControllerTests {
	
	@Autowired
	private MockMvc mvc;
	
	@MockitoBean
    private VipsRestService service;

    @Autowired
    private CodeTablesApiDelegateImpl controller;
    
    @Autowired
    ConfigProperties properties;
	
	@Test
    public void testCodeTablesApiGetSuccess() {
        
		GetCodetablesServiceResponse response = new GetCodetablesServiceResponse();
		response.setConfiguration(new ca.bc.gov.open.digitalformsapi.viirp.model.VipsConfigurationObj());

        Mockito.when(service.getCodeTableValues()).thenReturn(response);
        ResponseEntity<GetCodetablesServiceResponse> controllerResponse = controller.getCodeTables(null);
        GetCodetablesServiceResponse result = controllerResponse.getBody();
        Mockito.verify(service).getCodeTableValues();
        assertEquals(response.getConfiguration(), result.getConfiguration());
    }
	
	@Test
	public void localProcessingFailureRemains500WithNoDiagnosticLeak() throws Exception {
		Mockito.when(service.getCodeTableValues()).thenThrow(
				new DigitalFormsException("Internal file failure", new IOException("sensitive local path")));
		
	    mvc.perform(get("/codetables")
            		.contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
			.andExpect(content().contentType("application/problem+json"))
	    	.andExpect(result -> assertTrue(result.getResolvedException() instanceof DigitalFormsException))
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.detail").value("Internal Server Error"));
	}
}
