package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ca.bc.gov.open.digitalformsapi.viirp.UnitTestUtilities;
import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.model.StoreVIPSDocument;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsDocumentResponse;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import ca.bc.gov.open.jag.ordsvipsclient.api.DocumentApi;
import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiException;
import ca.bc.gov.open.jag.ordsvipsclient.api.model.VipsDocumentOrdsResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class DocumentsApiControllerTests {
	
	@Autowired
	private MockMvc mvc;

    @InjectMocks
    private DocumentsApiDelegateImpl controller;
    
    @Mock
    private DocumentApi documentApi;
    
    @Mock
    ConfigProperties properties;
    
    private VipsDocumentOrdsResponse goodOrds; //200
    
    private VipsDocumentOrdsResponse badOrds; //200 but ORDS sent 1 which indicates failure. 
    
    private StoreVIPSDocument goodStoreVIPSDocument;
    
    private StoreVIPSDocument badStoreVIPSDocument;
    
    @BeforeEach
	public void init() {
    	
		MockitoAnnotations.openMocks(this);
		
		// Mock ORDS good response
		goodOrds = new VipsDocumentOrdsResponse();
		goodOrds.setDocumentId("567");
		goodOrds.setStatusCode(DigitalFormsConstants.VIPSORDS_SUCCESS_CD); 
		goodOrds.setStatusMessage("Success"); 
		
		// Mock ORDS bad response
		badOrds = new VipsDocumentOrdsResponse();
		badOrds.setStatusCode(DigitalFormsConstants.VIPSORDS_GENERAL_FAILURE_CD); 
		
		//Mock delegate request type - good case
		goodStoreVIPSDocument = new StoreVIPSDocument();
		goodStoreVIPSDocument.setFileObject(new String("abc").getBytes());
		goodStoreVIPSDocument.setMimeSubType("pdf");
		goodStoreVIPSDocument.setMimeType("application");
		goodStoreVIPSDocument.setTypeCode("MV2702A");
		
		//Mock delegate request type - bad case (missing Mime Type)
		badStoreVIPSDocument = new StoreVIPSDocument();
		badStoreVIPSDocument.setFileObject(new String("abc").getBytes());
		badStoreVIPSDocument.setMimeSubType(null);
		badStoreVIPSDocument.setMimeType("application");
		badStoreVIPSDocument.setTypeCode("MV2702A");
		
	}
    
    @DisplayName("POST, Success - Store Document")
    @Test
    public void testDocumentsCorrelationIdPostSuccess() throws ApiException {
        
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
		
		when(documentApi.storeDocumentPost(any(), any(), any(), any(), any(), any(), any(), any()))
		.thenReturn(goodOrds);
		
		when(properties.getVipsRestApiCredentialsGuid())
		.thenReturn("guid");
		
		// Create successful call and validate response 
        ResponseEntity<VipsDocumentResponse> controllerResponse = controller.documentsCorrelationIdPost(correlationId, goodStoreVIPSDocument);
        VipsDocumentResponse result = controllerResponse.getBody();
        Assertions.assertEquals("567", result.getDocumentId());
        
    }
    
    @DisplayName("POST, Failure - Bad ORDS response, 500 expected")
    @Test
    public void testDocumentsCorrelationIdPostOrdsFailure() throws ApiException {
        
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
		
		when(documentApi.storeDocumentPost(any(), any(), any(), any(), any(), any(), any(), any()))
		.thenReturn(badOrds);
		
		when(properties.getVipsRestApiCredentialsGuid())
		.thenReturn("guid");
		
		// Ensure Digital Form Exception type is thrown in this case
		Assertions.assertThrows(DigitalFormsException.class, () -> {
					controller.documentsCorrelationIdPost(correlationId, goodStoreVIPSDocument);
				});
	    
    }
    
    @DisplayName("POST, Failure - Data missing from the store document request body, 500 expected")
    @Test
    public void testDocumentsCorrelationIdPostBadRequest() throws Exception {
        
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
		
	    mvc.perform( MockMvcRequestBuilders
	    	      .post("/documents/{correlationId}", correlationId)
	    	      .content(UnitTestUtilities.asJsonString(badStoreVIPSDocument))
	    	      .contentType(MediaType.APPLICATION_JSON)
	    	      .accept(MediaType.APPLICATION_JSON))
	    	      .andExpect(status().isInternalServerError());

    }
    
}
