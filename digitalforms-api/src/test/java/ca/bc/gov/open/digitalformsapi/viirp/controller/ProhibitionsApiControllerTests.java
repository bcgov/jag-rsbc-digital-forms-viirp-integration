package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ca.bc.gov.open.digitalformsapi.viirp.utils.TemporalJson;
import ca.bc.gov.open.digitalformsapi.viirp.utils.UnitTestUtilities;
import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibitionResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.AddressCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.LicenceCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.ProhibitionCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.Prohibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.RegistrationCreate;
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

    @Mock
    private ca.bc.gov.open.digitalformsapi.viirp.service.NoticeConflictService noticeConflicts;

    @Mock
    private HttpServletRequest request;

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
		Mockito.when(request.getContextPath()).thenReturn("");
		
		// Mock POST Prohibition body 
		createProhibition = new CreateProhibition();
		ProhibitionCreate vipsProhibitionCreate = new ProhibitionCreate();
		vipsProhibitionCreate.setCityNm("VICTORIA");
		vipsProhibitionCreate.setDreEvaluationCds(null);
		vipsProhibitionCreate.setEffectiveDt(LocalDate.parse("2018-10-11"));
		vipsProhibitionCreate.setExpiryDt(null);
		vipsProhibitionCreate.setIncidentDtm(OffsetDateTime.parse("2018-11-05T00:00:00.000-07:00"));
		vipsProhibitionCreate.setNoticeServedDt(LocalDate.parse("2018-10-07"));
		vipsProhibitionCreate.setNoticeSubjectCd("PERS");
		vipsProhibitionCreate.setNoticeTypeCd("UL");
		vipsProhibitionCreate.setOriginalCauseCd("IRPINDEF");
		vipsProhibitionCreate.setPoliceDetachmentId(304L);
		vipsProhibitionCreate.setPoliceFileNo("802");
		vipsProhibitionCreate.setPoliceOfficerNm("Police Name");
		vipsProhibitionCreate.setPoliceFileNo("1234");
		vipsProhibitionCreate.setProhibitionNoticeNo("21900601");
		vipsProhibitionCreate.setProvinceCd("BC");
		vipsProhibitionCreate.setStreetDetailsTxt("Some place");
		vipsProhibitionCreate.setTempDlEffectiveDt(null);
		vipsProhibitionCreate.setTempDlExpiryDt(null);			
		
		RegistrationCreate vipsRegistrationCreate = new RegistrationCreate();
		vipsRegistrationCreate.setDataSourceCd("VIPS");
		vipsRegistrationCreate.setFirstGivenNm("AARON");
		vipsRegistrationCreate.setIcbcEnterpriseId("123");
		vipsRegistrationCreate.setRegistrationRoleCd("REGOWN");
		vipsRegistrationCreate.setSecondGivenNm("CHRISTOPHER");
		vipsRegistrationCreate.setSurnameNm("SMITH");
		
		List<AddressCreate> vipsAddressArray = new ArrayList<>(); 
		AddressCreate vipsAddressCreateObj = new AddressCreate();
		vipsAddressCreateObj.setAddressFirstLineTxt("2018 Warner Bros. Pl");
		vipsAddressCreateObj.setCityNm("Burbank");
		vipsAddressCreateObj.setCountryNm("USA");
		vipsAddressCreateObj.setPostalCodeTxt("96321");
		vipsAddressCreateObj.setProvinceCd("CA");
		vipsAddressCreateObj.setRegistrationAddressTypeCd("MAIL");
		
		LicenceCreate vipsLicenceCreateObj = new LicenceCreate();

		vipsLicenceCreateObj.setBirthDt(LocalDate.parse("1971-04-08"));
		vipsLicenceCreateObj.setDriverLicenceNo("4771979");
		vipsLicenceCreateObj.setDlJurisdictionCd("FL");
		
		vipsRegistrationCreate.setLicence(vipsLicenceCreateObj);
		vipsRegistrationCreate.setAddresses(vipsAddressArray);
		
		List<Long> vipsDocumentIdArray = new ArrayList<>();
		
		createProhibition.setProhibition(vipsProhibitionCreate); // 1
		createProhibition.setRegistration(vipsRegistrationCreate); // 2
		createProhibition.setDocumentIds(vipsDocumentIdArray); // 3
		
		
		//Mock VIPS WS Notice number for Prohibition Search response
		vipsSearch = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse();
		List<ProhibitionSearchResponseType> results = new ArrayList<ProhibitionSearchResponseType>();
		ProhibitionSearchResponseType obj = new ProhibitionSearchResponseType();
		obj.setProhibitionId(DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID);
		results.add(obj);
		vipsSearch.setResult(results);
				
		// Mock VIPS WS GET Prohibition response. 
		vipsProhibition = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse();
		Prohibition vipsProhibitionObj = new Prohibition();
		vipsProhibitionObj.setCityNm("VICTORIA");
		vipsProhibitionObj.setCancelled(false);
		vipsProhibition.setResult(vipsProhibitionObj);
		
	}
    
    @DisplayName("POST, Success - Prohibition Delegate")
    @Test
    public void testProhibitionCreateApiPOSTSuccess() {
        
		// Mock underlying VIPS REST service 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse();
		response.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		response.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
		response.setProhibitionId(123L);
        Mockito.when(service.createProhibition(createProhibition)).thenReturn(response);
        
        // Create successful call and validate response 
        ResponseEntity<CreateProhibitionResponse> controllerResponse = controller.createProhibition(createProhibition, null);
        CreateProhibitionResponse result = controllerResponse.getBody();
        Mockito.verify(service).createProhibition(createProhibition);
		assertEquals(123L, result.getProhibitionId());
        assertEquals("/prohibitions/21900601", controllerResponse.getHeaders().getLocation().toString());
    }
    
    
    @DisplayName("POST, VIPS WS General failure  - Prohibition Delegate")
    @Test
    public void testProhibitionCreateApiPOSTVipsWSGeneralFailure() {
        
		// Mock underlying VIPS REST service to send VIPS general failure. 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse();
		response.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
		Mockito.when(service.createProhibition(createProhibition)).thenReturn(response);
        
		// Ensure Digital Form Exception type is thrown in this case
		assertThrows(DigitalFormsException.class, () -> {
			controller.createProhibition(createProhibition, null);
		});
    }
    
    @DisplayName("POST, VIPS JAVA failure  - Prohibition Delegate")
    @Test
    public void testProhibitionCreateApiPOSTVipsJavaFailure() {
        
		// Mock underlying VIPS REST service to send VIPS general failure. 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse();
		response.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
		Mockito.when(service.createProhibition(createProhibition)).thenReturn(response);
        
		//Ensure Digital Form Exception type is thrown in this case
		assertThrows(DigitalFormsException.class, () -> {
			controller.createProhibition(createProhibition, null);
		});
	
    }
    
    @DisplayName("POST, BAD REQUEST  - Prohibition Delegate")  
    @Test
	public void testProhibitionCreateApiPOSTBadRequest() throws Exception {
    	
		String body = UnitTestUtilities.asJsonString(createProhibition);
		String invalid = body.replace("\"2018-11-05T00:00:00.000-07:00\"", "\"2021-10-22T0a:23:23.100-07:00\"");
		assertNotEquals(body, invalid);
		
	    mvc.perform( MockMvcRequestBuilders
	    	      .post("/prohibitions")
				  .content(invalid)
	    	      .contentType(MediaType.APPLICATION_JSON)
	    	      .accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("prohibition.incidentDtm"))
				.andExpect(jsonPath("$.errors[0].message").value(TemporalJson.DATE_TIME_MESSAGE));
	    
	}
   
    @DisplayName("GET, Search Success  - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETSearchSuccess() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long prohibitionId = DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Prohibition response in good case 
		vipsProhibition.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsProhibition.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.getProhibition(prohibitionId)).thenReturn(vipsProhibition);
        
        // Create successful search notice number call with prohibitionId and validate response 
        ResponseEntity<Prohibition> controllerResponse = controller.getProhibition(noticeNo, null);
        Prohibition result = controllerResponse.getBody();
        assertEquals("VICTORIA", result.getCityNm());
        
    }
    
    @DisplayName("GET, Search Not Found  - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETSearchNotFound() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	
    	ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse vipsSearchNotFound = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse();
		List<ProhibitionSearchResponseType> results = new ArrayList<ProhibitionSearchResponseType>();
		
		// Empty response object from VIPS means negative search result. 
		vipsSearchNotFound.setResult(results); 
    	
    	// Mock underlying VIPS REST Search 
		vipsSearchNotFound.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearchNotFound.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(noticeNo)).thenReturn(vipsSearchNotFound);
    	
    	// Ensure ResourceNotFoundException type is thrown in this case
    	assertThrows(ResourceNotFoundException.class, () -> {
    			controller.getProhibition(noticeNo, null);
    	});
    
    }
    
    @DisplayName("GET, Search Result size > 1 - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETSearchResultSizeGreaterThanOne() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    
    	//add one more prohibition ID to the search results.
    	ProhibitionSearchResponseType obj2 = new ProhibitionSearchResponseType();
		obj2.setProhibitionId(22222223L); 
		vipsSearch.getResult().add(obj2);
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	assertThrows(DigitalFormsException.class, () -> {
    			controller.getProhibition(noticeNo, null);
    	});
    
    }
    
    @DisplayName("GET, Search VIPS General Failure - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETSearchVIPSGenFailure() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
        Mockito.when(service.searchProhibition(noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	assertThrows(DigitalFormsException.class, () -> {
    			controller.getProhibition(noticeNo, null);
    	});
    	
    }
    
    @DisplayName("GET, Search VIPS Internal Java Failure - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETSearchVIPSInternalFailure() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
        Mockito.when(service.searchProhibition(noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	assertThrows(DigitalFormsException.class, () -> {
    			controller.getProhibition(noticeNo, null);
    	});
    	
    	
    }
    
    @DisplayName("GET, Prohibition Not Found - Impoundment Delegate")  
    @Test
	public void testProhibitionApiGETProhibitionNotFound() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long prohibitionId = DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Prohibition response returning no impoundment even though the
        // search return with an prohibition Id. (Very BAD case for VIPS if this ever happened)
        ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse vipsProhibitionNone = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse();
        vipsProhibitionNone.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
 		vipsProhibitionNone.setResult(null); // no prohibition in the response 
        
		vipsProhibition.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.getProhibition(prohibitionId)).thenReturn(vipsProhibitionNone);
        
        // Ensure ResourceNotFoundException type is thrown in this case
    	assertThrows(ResourceNotFoundException.class, () -> {
    			controller.getProhibition(noticeNo, null);
    	});
    	
    	
    }
    
    @DisplayName("GET, Prohibition General Failure - Impoundment Delegate")  
    @Test
	public void testProhibitionApiGETVIPSProhibitionGenFailure() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long prohibitionId = DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Prohibition response reporting general failure 
		vipsProhibition.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
	    Mockito.when(service.getProhibition(prohibitionId)).thenReturn(vipsProhibition);
	    
	    //DigitalFormsException ex = new DigitalFormsException("test");
	    
        // Ensure DigitalFormsException type is thrown in this case
    	assertThrows(DigitalFormsException.class, () -> {
    			controller.getProhibition(noticeNo, null);
    	});
    	
    	
    }
    
    @DisplayName("GET, Prohibition VIPS Internal Java Failure - Prohibition Delegate")  
    @Test
	public void testProhibitionApiGETVIPSProhibitionInternalFailure() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long prohibitionId = DigitalFormsConstants.UNIT_TEST_PROHIBITION_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchProhibition(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Prohibition response reporting internal Java failure 
		vipsProhibition.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
	    Mockito.when(service.getProhibition(prohibitionId)).thenReturn(vipsProhibition);
	    
        // Ensure DigitalFormsException type is thrown in this case
    	assertThrows(DigitalFormsException.class, () -> {
    			controller.getProhibition(noticeNo, null);
    	});
    	
   
    }
    
    
    @DisplayName("POST, Prohibition unsupported route - Prohibition Delegate")
    @Test
	public void testProhibitionApiPOSTUnsupportedRoute() throws Exception {
    
		   mvc.perform( MockMvcRequestBuilders
		  	      .post("/prohibitions/123456")
		  	      .content(UnitTestUtilities.asJsonString(createProhibition))
		  	      .contentType(MediaType.APPLICATION_JSON)
		  	      .accept(MediaType.APPLICATION_JSON))
		  	      .andExpect(status().isMethodNotAllowed());
    
		   mvc.perform( MockMvcRequestBuilders
			  	      .post("/prohibitions/123456/extra")
			  	      .content(UnitTestUtilities.asJsonString(createProhibition))
			  	      .contentType(MediaType.APPLICATION_JSON)
			  	      .accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound());
    }
    
    @DisplayName("POST, Prohibition missing body - Prohibition Delegate")  
    @Test
	public void testProhibitionApiPOSTVIPSProhibitionNoBody() throws Exception {
    
		   mvc.perform( MockMvcRequestBuilders
				.post("/prohibitions")
		  	      .contentType(MediaType.APPLICATION_JSON)
		  	      .accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
    
    }
    
}
