package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ca.bc.gov.open.digitalformsapi.viirp.utils.UnitTestUtilities;
import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsAddressCreateObj;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsLicenceCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsProhibitionCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsRegistrationCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.ProhibitionSearchResponseType;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc(addFilters = false)
public class ProhibitionsApiControllerTests {
	
	@Autowired
	private MockMvc mvc;
	
	@Mock
    private VipsRestService service;

    @InjectMocks
    private ProhibitionsApiDelegateImpl controller;
    
    private CreateProhibition createProhibition;
    
    private ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse vipsSearch;
    
    private ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse vipsProhibition; 
    
    @Autowired
    ConfigProperties properties;
    
    @BeforeEach
	public void init() {
    	
		MockitoAnnotations.openMocks(this);
		
		// Mock POST Prohibition body 
		createProhibition = new CreateProhibition();
		VipsProhibitionCreate vipsProhibitionCreate = new VipsProhibitionCreate();
		vipsProhibitionCreate.setCityNm("VICTORIA");
		vipsProhibitionCreate.setDreEvaluationCds(null);
		vipsProhibitionCreate.setEffectiveDt("2018-10-11T00:00:00.000-07:00");
		vipsProhibitionCreate.setExpiryDt(null);
		vipsProhibitionCreate.setIncidentDtm("2018-11-05T00:00:00.000-07:00");
		vipsProhibitionCreate.setNoticeServedDt("2018-10-07T00:00:00.000-07:00");
		vipsProhibitionCreate.setNoticeSubjectCd("PERS");
		vipsProhibitionCreate.setNoticeTypeCd("UL");
		vipsProhibitionCreate.setOriginalCauseCd("IRPINDEF");
		vipsProhibitionCreate.setPoliceDetatchmentId(304L);
		vipsProhibitionCreate.setPoliceFileNo("802");
		vipsProhibitionCreate.setPoliceOfficerNm("Police Name");
		vipsProhibitionCreate.setPoliceFileNo("1234");
		vipsProhibitionCreate.setProhibitionNoticeNo("21900601");
		vipsProhibitionCreate.setProvinceCd("BC");
		vipsProhibitionCreate.setStreetDetailsTxt("Some place");
		vipsProhibitionCreate.setTempDlEffectiveDt(null);
		vipsProhibitionCreate.setTempDlExpiryDt(null);			
		
		VipsRegistrationCreate vipsRegistrationCreate = new VipsRegistrationCreate();
		vipsRegistrationCreate.setDataSourceCd("VIPS");
		vipsRegistrationCreate.setFirstGivenNm("AARON");
		vipsRegistrationCreate.setIcbcEnterpriseId("123");
		vipsRegistrationCreate.setRegistrationRoleCd("REGOWN");
		vipsRegistrationCreate.setSecondGivenNm("CHRISTOPHER");
		vipsRegistrationCreate.setSurnameNm("SMITH");
		
		List<VipsAddressCreateObj> vipsAddressArray = new ArrayList<>(); 
		VipsAddressCreateObj vipsAddressCreateObj = new VipsAddressCreateObj();
		vipsAddressCreateObj.setAddressFirstLineTxt("2018 Warner Bros. Pl");
		vipsAddressCreateObj.setCityNm("Burbank");
		vipsAddressCreateObj.setCountryNm("USA");
		vipsAddressCreateObj.setPostalCodeTxt("96321");
		vipsAddressCreateObj.setProvinceCd("CA");
		vipsAddressCreateObj.setRegistrationAddressTypeCd("MAIL");
		
		VipsLicenceCreate vipsLicenceCreateObj = new VipsLicenceCreate();

		vipsLicenceCreateObj.setBirthDt("1971-04-08T00:00:00.000-07:00");
		vipsLicenceCreateObj.setDriverLicenceNo("4771979");
		vipsLicenceCreateObj.setDlJurisdictionCd("FL");
		
		vipsRegistrationCreate.setVipsLicenceCreateObj(vipsLicenceCreateObj);
		vipsRegistrationCreate.setVipsAddressArray(vipsAddressArray);
		
		List<Long> vipsDocumentIdArray = new ArrayList<>();
		
		createProhibition.setVipsProhibitionCreate(vipsProhibitionCreate); // 1
		createProhibition.setVipsRegistrationCreate(vipsRegistrationCreate); // 2
		createProhibition.setVipsDocumentIdArray(vipsDocumentIdArray); // 3
		
		
		//Mock VIPS WS Notice number for Prohibition Search response
		vipsSearch = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse();
		List<ProhibitionSearchResponseType> results = new ArrayList<ProhibitionSearchResponseType>();
		ProhibitionSearchResponseType obj = new ProhibitionSearchResponseType();
		obj.setProhibitionId(DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID);
		results.add(obj);
		vipsSearch.setResult(results);
				
		// Mock VIPS WS GET Prohibition response. 
		vipsProhibition = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse();
		ca.bc.gov.open.digitalformsapi.viirp.model.VipsProhibitionObj vipsProhibitionObj = new ca.bc.gov.open.digitalformsapi.viirp.model.VipsProhibitionObj();
		vipsProhibitionObj.setCityNm("VICTORIA");
		vipsProhibitionObj.setCancelled(false);
		vipsProhibition.setResult(vipsProhibitionObj);
		
	}
    
    @DisplayName("POST, Success - Prohibition Delegate")
    @Test
    public void testProhibitionCreateApiPOSTSuccess() {
        
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
		
		// Mock underlying VIPS REST service 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse();
		response.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		response.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.createProhibition(correlationId, createProhibition)).thenReturn(response);
        
        // Create successful call and validate response 
        ResponseEntity<CreateProhibitionServiceResponse> controllerResponse = controller.prohibitionsCorrelationIdPost(correlationId, createProhibition);
        CreateProhibitionServiceResponse result = controllerResponse.getBody();
        Mockito.verify(service).createProhibition(correlationId, createProhibition);
        assertEquals(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG, result.getRespMsg());
    }
    
    
    @DisplayName("POST, VIPS WS General failure  - Prohibition Delegate")
    @Test
    public void testProhibitionCreateApiPOSTVipsWSGeneralFailure() {
        
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
		
		// Mock underlying VIPS REST service to send VIPS general failure. 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse();
		response.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
		Mockito.when(service.createProhibition(correlationId, createProhibition)).thenReturn(response);
        
		// Ensure Digital Form Exception type is thrown in this case
		assertThrows(DigitalFormsException.class, () -> {
			controller.prohibitionsCorrelationIdPost(correlationId, createProhibition);
		});
    }
    
    @DisplayName("POST, VIPS JAVA failure  - Prohibition Delegate")
    @Test
    public void testProhibitionCreateApiPOSTVipsJavaFailure() {
        
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
		
		// Mock underlying VIPS REST service to send VIPS general failure. 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse();
		response.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
		Mockito.when(service.createProhibition(correlationId, createProhibition)).thenReturn(response);
        
		//Ensure Digital Form Exception type is thrown in this case
		assertThrows(DigitalFormsException.class, () -> {
			controller.prohibitionsCorrelationIdPost(correlationId, createProhibition);
		});
	
    }
    
    // TODO - This checks for 500 errors when validation fails. Why - Because of Springboot's inadequate validation handling. 
    // Currently the default 500 error is thrown when validation fails instead of the proper 400 BAD REQUEST. 
    // I don't have an immediate fix for this as it is complicated by the way we presently auto-generate the controller delegates
    // from the specification file.  
    
    @DisplayName("POST, BAD REQUEST  - Prohibition Delegate")  
    @Test
	public void testProhibitionCreateApiPOSTBadRequest() throws Exception {
    	
    	createProhibition.getVipsProhibitionCreate().setIncidentDtm("2021-10-22T0a:23:23.100-07:00"); // Doesn't match regex for date/time. 
		
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
	    
	    mvc.perform( MockMvcRequestBuilders
	    	      .post("/prohibitions/{correlationId}", correlationId)
	    	      .content(UnitTestUtilities.asJsonString(createProhibition))
	    	      .contentType(MediaType.APPLICATION_JSON)
	    	      .accept(MediaType.APPLICATION_JSON))
	    	      .andExpect(status().isInternalServerError());
	    
	}
   
    @DisplayName("GET, Search Success  - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETSearchSuccess() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long prohibitionId = DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Prohibition response in good case 
		vipsProhibition.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsProhibition.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.getProhibition(correlationId, prohibitionId)).thenReturn(vipsProhibition);
        
        // Create successful search notice number call with prohibitionId and validate response 
        ResponseEntity<GetProhibitionServiceResponse> controllerResponse = controller.prohibitionsNoticeNoCorrelationIdGet(noticeNo, correlationId);
        GetProhibitionServiceResponse result = controllerResponse.getBody();
        assertEquals("VICTORIA", result.getResult().getCityNm());
        
    }
    
    @DisplayName("GET, Search Not Found  - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETSearchNotFound() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	
    	ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse vipsSearchNotFound = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse();
		List<ProhibitionSearchResponseType> results = new ArrayList<ProhibitionSearchResponseType>();
		
		// Empty response object from VIPS means negative search result. 
		vipsSearchNotFound.setResult(results); 
    	
    	// Mock underlying VIPS REST Search 
		vipsSearchNotFound.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearchNotFound.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(correlationId, noticeNo)).thenReturn(vipsSearchNotFound);
    	
    	// Ensure ResourceNotFoundException type is thrown in this case
    	assertThrows(ResourceNotFoundException.class, () -> {
    			controller.prohibitionsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    
    }
    
    @DisplayName("GET, Search Result size > 1 - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETSearchResultSizeGreaterThanOne() throws Exception {
    	
       	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    
    	//add one more prohibition ID to the search results.
    	ProhibitionSearchResponseType obj2 = new ProhibitionSearchResponseType();
		obj2.setProhibitionId(22222223L); 
		vipsSearch.getResult().add(obj2);
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	assertThrows(DigitalFormsException.class, () -> {
    			controller.prohibitionsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    
    }
    
    @DisplayName("GET, Search VIPS General Failure - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETSearchVIPSGenFailure() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
        Mockito.when(service.searchProhibition(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	assertThrows(DigitalFormsException.class, () -> {
    			controller.prohibitionsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    	
    }
    
    @DisplayName("GET, Search VIPS Internal Java Failure - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETSearchVIPSInternalFailure() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
        Mockito.when(service.searchProhibition(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	assertThrows(DigitalFormsException.class, () -> {
    			controller.prohibitionsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    	
    	
    }
    
    @DisplayName("GET, Prohibition Not Found - Impoundment Delegate")  
    @Test
	public void testProhibitionApiGETProhibitionNotFound() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long prohibitionId = DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Prohibition response returning no impoundment even though the
        // search return with an prohibition Id. (Very BAD case for VIPS if this ever happened)
        ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse vipsProhibitionNone = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse();
        vipsProhibitionNone.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
 		vipsProhibitionNone.setResult(null); // no prohibition in the response 
        
		vipsProhibition.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.getProhibition(correlationId, prohibitionId)).thenReturn(vipsProhibitionNone);
        
        // Ensure ResourceNotFoundException type is thrown in this case
    	assertThrows(ResourceNotFoundException.class, () -> {
    			controller.prohibitionsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    	
    	
    }
    
    @DisplayName("GET, Prohibition General Failure - Impoundment Delegate")  
    @Test
	public void testProhibitionApiGETVIPSProhibitionGenFailure() throws Exception {
    	
      	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long prohibitionId = DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Prohibition response reporting general failure 
		vipsProhibition.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
	    Mockito.when(service.getProhibition(correlationId, prohibitionId)).thenReturn(vipsProhibition);
	    
	    //DigitalFormsException ex = new DigitalFormsException("test");
	    
        // Ensure DigitalFormsException type is thrown in this case
    	assertThrows(DigitalFormsException.class, () -> {
    			controller.prohibitionsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    	
    	
    }
    
    @DisplayName("GET, Prohibition VIPS Internal Java Failure - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETVIPSProhibitionInternalFailure() throws Exception {
    	
       	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long prohibitionId = DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Prohibition response reporting internal Java failure 
		vipsProhibition.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
	    Mockito.when(service.getProhibition(correlationId, prohibitionId)).thenReturn(vipsProhibition);
	    
        // Ensure DigitalFormsException type is thrown in this case
    	assertThrows(DigitalFormsException.class, () -> {
    			controller.prohibitionsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    	
   
    }
    
    
    @DisplayName("GET, Prohibition missing CorrelationId or Notice No - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETVIPSProhibitionMissingCorrlelationOrNotice() throws Exception {
    
		   mvc.perform( MockMvcRequestBuilders
		  	      .post("/prohibitions")
		  	      .content(UnitTestUtilities.asJsonString(createProhibition))
		  	      .contentType(MediaType.APPLICATION_JSON)
		  	      .accept(MediaType.APPLICATION_JSON))
		  	      .andExpect(status().isNotFound());
    
		   mvc.perform( MockMvcRequestBuilders
			  	      .post("/prohibitions/123456")
			  	      .content(UnitTestUtilities.asJsonString(createProhibition))
			  	      .contentType(MediaType.APPLICATION_JSON)
			  	      .accept(MediaType.APPLICATION_JSON))
			  	      .andExpect(status().isInternalServerError());
    }
    
    @DisplayName("POST, Prohibition missing body - Prohibition Delegate")  
    @Test
	public void testProhibitionApiPOSTVIPSProhibitionNoBody() throws Exception {
    
		   mvc.perform( MockMvcRequestBuilders
		  	      .post("/prohibitions/1234567/22222222")
		  	      .contentType(MediaType.APPLICATION_JSON)
		  	      .accept(MediaType.APPLICATION_JSON))
		  	      .andExpect(status().isInternalServerError());
    
    }
    
}
