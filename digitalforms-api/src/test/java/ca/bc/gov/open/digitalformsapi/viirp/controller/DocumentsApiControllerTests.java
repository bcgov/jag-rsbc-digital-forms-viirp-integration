package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.model.StoreVIPSDocument;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsDocumentResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsGetDocumentByIdResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsNoticeObj;
import ca.bc.gov.open.digitalformsapi.viirp.model.AssociateDocumentToNoticeServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import ca.bc.gov.open.jag.ordsvipsclient.api.DocumentApi;
import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiException;
import ca.bc.gov.open.jag.ordsvipsclient.api.model.VipsDocumentOrdsResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class DocumentsApiControllerTests {

	@Autowired
	private MockMvc mvc;
	
	@Mock
    private VipsRestService service;
	
    @InjectMocks
    private DocumentsApiDelegateImpl controller;
    
    @Mock
    private DocumentApi documentApi;
    
    private VipsDocumentOrdsResponse goodOrds; //200
    
    private VipsDocumentOrdsResponse badOrds; //200 but ORDS sent 1 which indicates failure. 
    
    private StoreVIPSDocument goodStoreVIPSDocument;
    
    private StoreVIPSDocument badStoreVIPSDocument;
    
    private VipsGetDocumentByIdResponse vipsDocumentResponse;
    
    private VipsNoticeObj goodVipsNoticeObj; 
    
    private VipsNoticeObj missingNoticeNoVipsNoticeObj;
    
    private VipsNoticeObj missingNoticeTypeCdVipsNoticeObj;
    
    private ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse goodDocumentAssociateResponse; 
    
    private ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse failNotFoundDocumentAssociateResponse; 
    
    private ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse vipsWSGenFailureDocumentAssociateResponse; 
    
    @Mock
    ConfigProperties properties;

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
		
		// Mock VIPS WS GET document response. 
		vipsDocumentResponse = new VipsGetDocumentByIdResponse();
		
		// Mock VIPS WS POST Document Notice Number Association req and response objects. 
		goodVipsNoticeObj = new VipsNoticeObj();
		goodVipsNoticeObj.setNoticeNo("22909091");
		goodVipsNoticeObj.setNoticeTypeCd("IMP");
		
		missingNoticeNoVipsNoticeObj = new VipsNoticeObj();
		missingNoticeNoVipsNoticeObj.setNoticeTypeCd("IMP");
		
		missingNoticeTypeCdVipsNoticeObj = new VipsNoticeObj();
		missingNoticeTypeCdVipsNoticeObj.setNoticeNo("2290102");
		
		goodDocumentAssociateResponse = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse();
		goodDocumentAssociateResponse.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
		
		failNotFoundDocumentAssociateResponse = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse();
		failNotFoundDocumentAssociateResponse.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
		failNotFoundDocumentAssociateResponse.setRespMsg(DigitalFormsConstants.VIPS_NOTICE_NOT_FOUND);
		
		vipsWSGenFailureDocumentAssociateResponse = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse();
		vipsWSGenFailureDocumentAssociateResponse.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
		vipsWSGenFailureDocumentAssociateResponse.setRespMsg("something bad happened");
		
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
    
    @DisplayName("GET, Get Document Success  - Documents API Delegate")  
    @Test
	public void testDocumentsDocumentIdCorrelationIdGetSuccess() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	Long documentId = DigitalFormsConstants.UNIT_TEST_DOCUMENT_ID;
    	
    	Base64.Encoder enc = Base64.getEncoder();
		String testStr = "77+9x6s=";
		// encode data using BASE64
		String encodedBase64 = enc.encodeToString(testStr.getBytes());
		vipsDocumentResponse.setDocument(encodedBase64);
		
		// Mock underlying VIPS REST get document request that returns base64
        Mockito.when(service.getDocumentAsBase64(correlationId, documentId)).thenReturn(vipsDocumentResponse);
        
        // Return base64 document string successfully from the call with documentId and validate response 
        ResponseEntity<VipsGetDocumentByIdResponse> controllerResponse = controller.documentsDocumentIdCorrelationIdGet(correlationId, documentId);
        VipsGetDocumentByIdResponse result = controllerResponse.getBody();
        Mockito.verify(service).getDocumentAsBase64(correlationId, documentId);
        Assert.assertEquals(encodedBase64, result.getDocument());
    }
    
    @DisplayName("GET, Get Document Not Found  - Documents API Delegate")  
    @Test
	public void testDocumentsDocumentIdCorrelationIdGetNotFound() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	Long documentId = DigitalFormsConstants.UNIT_TEST_DOCUMENT_ID;
		
		// Empty response object from VIPS means document is not found for the given documentID
		vipsDocumentResponse.setDocument("");
		
		// Mock underlying VIPS REST get document request that returns empty result
        Mockito.when(service.getDocumentAsBase64(correlationId, documentId)).thenReturn(vipsDocumentResponse);
    	
    	// Ensure ResourceNotFoundException type is thrown in this case
    	Assertions.assertThrows(ResourceNotFoundException.class, () -> {
    			controller.documentsDocumentIdCorrelationIdGet(correlationId, documentId);
    	});
    }
    
    @DisplayName("GET, Get Document Internal Server Error  - Documents API Delegate")  
    @Test
	public void testDocumentsDocumentIdCorrelationIdGetInvalidBase64() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	Long documentId = DigitalFormsConstants.UNIT_TEST_DOCUMENT_ID;
		
		// Set an invalid base64 string to the response from VIPS 
		vipsDocumentResponse.setDocument("%%%%%");
		
		// Mock underlying VIPS REST get document request that returns bad data due to internal error
        Mockito.when(service.getDocumentAsBase64(correlationId, documentId)).thenReturn(vipsDocumentResponse);
    	
    	// Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.documentsDocumentIdCorrelationIdGet(correlationId, documentId);
    	});
    }
    
    @DisplayName("POST, Create Document Id to Notice Number Association success - Documents API Delegate")  
    @Test
	public void documentsAssociationNoticeDocumentIdPostSuccess() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	Long documentId = 606L;
    		
    	// Mock underlying VIPS REST get document request that returns bad data due to internal error
        Mockito.when(service.createDocumentAsociationPost(documentId, goodVipsNoticeObj)).thenReturn(goodDocumentAssociateResponse); 
        
        // Create successful call and validate response 
        ResponseEntity<AssociateDocumentToNoticeServiceResponse> controllerResponse = controller.documentsAssociationNoticeDocumentIdCorrelationIdPost(correlationId, documentId, goodVipsNoticeObj);
        AssociateDocumentToNoticeServiceResponse result = controllerResponse.getBody();
        Assertions.assertEquals(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG, result.getStatusMessage());
    }
    
    @DisplayName("POST, Create Document Id to Notice Number Association failure, Notice No not found - Documents API Delegate")  
    @Test
	public void documentsAssociationNoticeDocumentIdPostFailureNotFound() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	Long documentId = 606L;
    		
    	// Mock underlying VIPS REST get document request that returns bad data due to internal error
        Mockito.when(service.createDocumentAsociationPost(documentId, goodVipsNoticeObj)).thenReturn(failNotFoundDocumentAssociateResponse); 
        
        // Ensure ResourceNotFoundException type is thrown in this case
    	Assertions.assertThrows(ResourceNotFoundException.class, () -> {
    			controller.documentsAssociationNoticeDocumentIdCorrelationIdPost(correlationId, documentId, goodVipsNoticeObj);
    	});
    }
    
    @DisplayName("POST, Create Document Id to Notice Number Association failure, VIPS WS general failure - Documents API Delegate")  
    @Test
	public void documentsAssociationNoticeDocumentIdPostVIPSWSGenFailure() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	Long documentId = 606L;
    		
    	// Mock underlying VIPS REST get document request that returns bad data due to internal error
        Mockito.when(service.createDocumentAsociationPost(documentId, goodVipsNoticeObj)).thenReturn(vipsWSGenFailureDocumentAssociateResponse); 
        
        // Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.documentsAssociationNoticeDocumentIdCorrelationIdPost(correlationId, documentId, goodVipsNoticeObj);
    	});
    }
    
    @DisplayName("POST, Create Document Id to Notice Number Association failure, missing noticeNo - Documents API Delegate")  
    @Test
	public void documentsAssociationNoticeDocumentIdPostMissingNoticeNoFailure() throws Exception {
    	
    	 mvc.perform( MockMvcRequestBuilders
	    	      .post("/documents/association/notice/606/{correlationId}", DigitalFormsConstants.UNIT_TEST_CORRELATION_ID)
	    	      .content(UnitTestUtilities.asJsonString(missingNoticeNoVipsNoticeObj))
	    	      .contentType(MediaType.APPLICATION_JSON)
	    	      .accept(MediaType.APPLICATION_JSON))
	    	      .andExpect(status().isInternalServerError());
    	
    }
    
    @DisplayName("POST, Create Document Id to Notice Number Association failure, missing noticeTypeCd - Documents API Delegate")  
    @Test
	public void documentsAssociationNoticeDocumentIdPostMissingNoticeTypeCdFailure() throws Exception {
    	
    	 mvc.perform( MockMvcRequestBuilders
	    	      .post("/documents/association/notice/606/{correlationId}", DigitalFormsConstants.UNIT_TEST_CORRELATION_ID)
	    	      .content(UnitTestUtilities.asJsonString(missingNoticeTypeCdVipsNoticeObj))
	    	      .contentType(MediaType.APPLICATION_JSON)
	    	      .accept(MediaType.APPLICATION_JSON))
	    	      .andExpect(status().isInternalServerError());
    	
    }
    
}
