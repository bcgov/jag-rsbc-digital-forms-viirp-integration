package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.model.Document;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateDocumentResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.ProhibitionSearchResponseType;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.VipsImpoundmentBasicsObj;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsOrdsService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import ca.bc.gov.open.digitalformsapi.viirp.utils.UnitTestUtilities;
import ca.bc.gov.open.jag.ordsvipsclient.api.model.VipsDocumentOrdsResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc(addFilters = false)
public class DocumentsApiControllerTests {

	@Autowired
	private MockMvc mvc;
	
	@Mock
    private VipsRestService service;
	
    @InjectMocks
    private DocumentsApiDelegateImpl controller;
    
    @Mock
    private VipsOrdsService vipsOrdsService;
    
    private VipsDocumentOrdsResponse goodOrds; //200
    
    private VipsDocumentOrdsResponse badOrds; //200 but ORDS sent 1 which indicates failure. 
    
    private MockMultipartFile pdfFile;

    @Mock
    private HttpServletRequest request;
    
    private ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse vipsSearch;
    
    private ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse vipsProhibitionSearch;
    
    private ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetDocumentsListServiceResponse vipsDocumentsResponse; 
    
    @BeforeEach
	public void init() throws Exception {
    	
		MockitoAnnotations.openMocks(this);
		when(request.getContextPath()).thenReturn("");

		// Mock ORDS good response
		goodOrds = new VipsDocumentOrdsResponse();
		goodOrds.setDocumentId("567");
		goodOrds.setStatusCode(DigitalFormsConstants.VIPSORDS_SUCCESS_CD); 
		goodOrds.setStatusMessage("Success"); 
		
		// Mock ORDS bad response
		badOrds = new VipsDocumentOrdsResponse();
		badOrds.setStatusCode(DigitalFormsConstants.VIPSORDS_GENERAL_FAILURE_CD); 
		
		byte[] pdf = UnitTestUtilities.samplePdf();
		pdfFile = new MockMultipartFile("file", "notice.pdf", "application/pdf", pdf);
		when(request.getParts()).thenReturn(List.of(new MockPart("file", "notice.pdf", pdf)));
		
		//Mock VIPS WS Notice number for Impoundment Search response
		vipsSearch = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse();
		List<VipsImpoundmentBasicsObj> results = new ArrayList<VipsImpoundmentBasicsObj>();
		VipsImpoundmentBasicsObj obj = new VipsImpoundmentBasicsObj();
		obj.setImpoundmentId(DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID);
		results.add(obj);
		vipsSearch.setResult(results);
		
		//Mock VIPS WS Notice number for Prohibition Search response
		vipsProhibitionSearch = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse();
		List<ProhibitionSearchResponseType> prohibitionResults = new ArrayList<ProhibitionSearchResponseType>();
		ProhibitionSearchResponseType obj2 = new ProhibitionSearchResponseType();
		obj2.setProhibitionId(DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID);
		prohibitionResults.add(obj2);
		vipsProhibitionSearch.setResult(prohibitionResults);
		
		// Mock VIPS WS GET Documents List response. 
		vipsDocumentsResponse = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetDocumentsListServiceResponse();
		List<Document> documentsList = new ArrayList<Document>();
		Document vipsDocumentObj = new Document();
		vipsDocumentObj.setDocumentId(DigitalFormsConstants.UNIT_TEST_DOCUMENT_ID);
		vipsDocumentObj.setAddToFileDtm(OffsetDateTime.parse("2018-10-22T00:00:00.000-07:00"));
		vipsDocumentObj.setDocumentTypeCd("MV2702A");
		documentsList.add(vipsDocumentObj);
		vipsDocumentsResponse.setResult(documentsList);
		
	}
    
    @DisplayName("POST, Success - Store Document")
    @Test
    public void testStoreDocumentSuccess() {
        
		when(vipsOrdsService.storeDocument(any(), any(), any(), any(), anyInt()))
		.thenReturn(goodOrds);
		
		// Create successful call and validate response 
        ResponseEntity<CreateDocumentResponse> controllerResponse =
                controller.storeDocument("MV2702A", pdfFile, null, null, null);
        CreateDocumentResponse result = controllerResponse.getBody();
        Assertions.assertEquals(567L, result.getDocumentId());
        assertEquals("/documents/567", controllerResponse.getHeaders().getLocation().toString());
        
    }
    
    @DisplayName("POST, Failure - Bad ORDS response, 502 expected")
    @Test
    public void testStoreDocumentOrdsFailure() {
        
		when(vipsOrdsService.storeDocument(any(), any(), any(), any(), anyInt()))
		.thenReturn(badOrds);
		
		// Ensure Digital Form Exception type is thrown in this case
		assertThrows(DigitalFormsException.class, () -> {
			controller.storeDocument("MV2702A", pdfFile, null, null, null);
		});
	    
    }
    
    @DisplayName("POST, Failure - Data missing from the store document request body, 400 expected")
    @Test
    public void testStoreDocumentBadRequest() throws Exception {
        
        mvc.perform(MockMvcRequestBuilders.multipart("/documents")
                .file(pdfFile)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

    }
    
    @DisplayName("GET, streams PDF with download headers - Documents API Delegate")
    @Test
    public void testGetDocumentSuccess() throws Exception {
        Long documentId = DigitalFormsConstants.UNIT_TEST_DOCUMENT_ID;
        byte[] pdf = UnitTestUtilities.samplePdf();
        var upstream = ResponseEntity.ok().contentLength(pdf.length)
                .body(Flux.<DataBuffer>just(DefaultDataBufferFactory.sharedInstance.wrap(pdf)));
        Mockito.when(service.getDocument(documentId)).thenReturn(upstream);

        var response = controller.getDocument(documentId, null);
        Assertions.assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        Assertions.assertEquals("attachment; filename=\"document-" + documentId + ".pdf\"",
                response.getHeaders().getFirst("Content-Disposition"));
        Assertions.assertEquals(pdf.length, response.getHeaders().getContentLength());
        try (var stream = response.getBody().getInputStream()) {
            Assertions.assertArrayEquals(pdf, stream.readAllBytes());
        }
        Mockito.verify(service).getDocument(documentId);
    }

    @DisplayName("GET, empty PDF stream is not found - Documents API Delegate")
    @Test
    public void testGetDocumentNotFound() {
        Long documentId = DigitalFormsConstants.UNIT_TEST_DOCUMENT_ID;
        Mockito.when(service.getDocument(documentId)).thenReturn(ResponseEntity.ok(Flux.empty()));
        Assertions.assertThrows(ResourceNotFoundException.class, () -> controller.getDocument(documentId, null));
    }
    
    @DisplayName("GET, Documents Meta List with Impoundment Id Success - Documents API Delegate")  
    @Test
	public void testDocumentsApiGETDocumentsWithImpoundmentIdSuccess() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Documents response in good case 
        vipsDocumentsResponse.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
        vipsDocumentsResponse.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.getDocumentsMetaList(impoundmentId, null)).thenReturn(vipsDocumentsResponse);
        
        // Create successful get documents call with noticeNo and validate response 
        ResponseEntity<List<Document>> controllerResponse =
            controller.listImpoundmentDocuments(noticeNo, null);
        List<Document> result = controllerResponse.getBody();
        Assertions.assertTrue(result.size() > 0);
        Assertions.assertEquals("MV2702A", result.get(0).getDocumentTypeCd());
    }
    
    @DisplayName("GET, Documents Meta List with Prohibition Id Success - Documents API Delegate")  
    @Test
	public void testDocumentsApiGETDocumentsWithProhibitionIdSuccess() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long prohibitionId = DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID;
    	
		// Mock underlying VIPS REST Prohibition Search
		vipsProhibitionSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsProhibitionSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(noticeNo)).thenReturn(vipsProhibitionSearch);
        
        // Mock underlying VIPS REST Get Documents response in good case 
        vipsDocumentsResponse.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
        vipsDocumentsResponse.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.getDocumentsMetaList(null, prohibitionId)).thenReturn(vipsDocumentsResponse);
        
        // Create successful get documents call with noticeNo and validate response 
        ResponseEntity<List<Document>> controllerResponse =
                controller.listProhibitionDocuments(noticeNo, null);
        List<Document> result = controllerResponse.getBody();
        Assertions.assertTrue(result.size() > 0);
        Assertions.assertEquals("MV2702A", result.get(0).getDocumentTypeCd());
    }
    
    @DisplayName("GET, Empty document collection - Documents API Delegate")
    @Test
	public void testDocumentsApiGETDocumentsNotFound() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Documents response returning no documents in the response list even though the
        // search return with an impoundment Id.
        ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetDocumentsListServiceResponse vipsEmptyDocumentsResponse = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetDocumentsListServiceResponse();
        vipsEmptyDocumentsResponse.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        vipsEmptyDocumentsResponse.setResult(null); // no documents in the response 
        
        vipsDocumentsResponse.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.getDocumentsMetaList(impoundmentId, null)).thenReturn(vipsEmptyDocumentsResponse);
        
        var response = controller.listImpoundmentDocuments(noticeNo, null);
        assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(response.getBody().isEmpty());
    }
    
    @DisplayName("GET, Documents General Failure - Documents API Delegate")  
    @Test
	public void testDocumentsApiGETDocumentstGenFailure() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Documents response reporting general failure 
        vipsDocumentsResponse.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
	    Mockito.when(service.getDocumentsMetaList(impoundmentId, null)).thenReturn(vipsDocumentsResponse);
	    
        // Ensure DigitalFormsException type is thrown in this case
        Assertions.assertThrows(DigitalFormsException.class, () -> {
            controller.listImpoundmentDocuments(noticeNo, null);
        });
    }
    
    @DisplayName("GET, Documents VIPS Internal Java Failure - Documents API Delegate")  
    @Test
	public void testImpoundmentApiGETVIPSImpoundmentInternalFailure() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Documents response reporting internal Java failure
        vipsDocumentsResponse.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
        Mockito.when(service.getDocumentsMetaList(impoundmentId, null)).thenReturn(vipsDocumentsResponse);

        Assertions.assertThrows(DigitalFormsException.class,
                () -> controller.listImpoundmentDocuments(noticeNo, null));
    }
    
}
