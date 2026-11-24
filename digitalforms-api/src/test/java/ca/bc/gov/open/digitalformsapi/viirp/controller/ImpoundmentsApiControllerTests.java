package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundmentResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.AddressCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.Impoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.ImpoundmentCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.LicenceCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.RegistrationCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VehicleCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.VipsImpoundmentBasicsObj;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import ca.bc.gov.open.digitalformsapi.viirp.utils.TemporalJson;
import ca.bc.gov.open.digitalformsapi.viirp.utils.UnitTestUtilities;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc(addFilters = false)
public class ImpoundmentsApiControllerTests {
	
	@Autowired
	private MockMvc mvc;
	
	@Mock
    private VipsRestService service;

    @Mock
    private ca.bc.gov.open.digitalformsapi.viirp.service.NoticeConflictService noticeConflicts;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ImpoundmentsApiDelegateImpl controller;
    
    private CreateImpoundment createImpoundment;
    
    private ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse vipsSearch;
    
    private ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse vipsImpoundment; 
    
    @Autowired
    ConfigProperties properties;
    
    @BeforeEach
	public void init() {
    	
		MockitoAnnotations.openMocks(this);
		Mockito.when(request.getContextPath()).thenReturn("");
		
		// Mock POST Impoundment body 
		createImpoundment = new CreateImpoundment();
		ImpoundmentCreate vipsImpoundmentCreate = new ImpoundmentCreate();
		vipsImpoundmentCreate.setDlJurisdictionCd("BC");
		vipsImpoundmentCreate.setImpoundmentDt(LocalDate.parse("2021-10-08"));
		vipsImpoundmentCreate.setImpoundmentNoticeNo("12345678");
		List<String> originalCauseCds = Arrays.asList("STUNT");
		vipsImpoundmentCreate.setOriginalCauseCds(originalCauseCds);
		vipsImpoundmentCreate.setJudicialOrder(false);
		vipsImpoundmentCreate.setPoliceDetachmentId(304L);
		vipsImpoundmentCreate.setPoliceOfficerNm("Jones");
		vipsImpoundmentCreate.setPoliceOfficerNo("420");
		vipsImpoundmentCreate.setPoliceFileNo("29");
		vipsImpoundmentCreate.setSeizureLocationTxt("Victoria");				

		createImpoundment.setImpoundment(vipsImpoundmentCreate);	
		
		List<RegistrationCreate> vipsRegistrationCreateArray = new ArrayList<>();
		
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
		vipsAddressCreateObj.setRegistrationAddressTypeCd(AddressCreate.RegistrationAddressTypeCdEnum.MAIL);
		
		LicenceCreate vipsLicenceCreateObj = new LicenceCreate();

		vipsLicenceCreateObj.setBirthDt(LocalDate.parse("1971-04-08"));
		vipsLicenceCreateObj.setDriverLicenceNo("4771979");
		vipsLicenceCreateObj.setDlJurisdictionCd("FL");
		
		vipsRegistrationCreate.setLicence(vipsLicenceCreateObj);
		vipsRegistrationCreate.setAddresses(vipsAddressArray);
		
		VehicleCreate vipsVehicleCreate = new VehicleCreate();
		vipsVehicleCreate.setLicencePlateNo("RGAMB9");
		vipsVehicleCreate.setLpDecalValidYy(2018L);
		vipsVehicleCreate.setLpJurisdictionCd("BC");
		vipsVehicleCreate.setManufacturedYy(1978L);
		vipsVehicleCreate.setRegistrationNo("8762133");
		vipsVehicleCreate.setVehicleColourTxt("GREEN");
		vipsVehicleCreate.setVehicleIdentificationNo("2T9DCAAC3W1065956");
		vipsVehicleCreate.setVehicleMakeTxt("AMC");
		vipsVehicleCreate.setVehicleModelTxt("Sport");
		vipsVehicleCreate.setVehicleTypeCd("1");
		
		
		createImpoundment.setImpoundment(vipsImpoundmentCreate); // 1
		createImpoundment.setRegistrations(vipsRegistrationCreateArray); // 2
		createImpoundment.setVehicle(vipsVehicleCreate); // 3
		
		//Mock VIPS WS Notice number for Impoundment Search response
		vipsSearch = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse();
		List<VipsImpoundmentBasicsObj> results = new ArrayList<VipsImpoundmentBasicsObj>();
		VipsImpoundmentBasicsObj obj = new VipsImpoundmentBasicsObj();
		obj.setImpoundmentId(DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID);
		results.add(obj);
		vipsSearch.setResult(results);
		
		// Mock VIPS WS GET Impoundment response. 
		vipsImpoundment = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse();
		Impoundment vipsImpoundObj = new Impoundment();
		vipsImpoundObj.setDlJurisdictionCd("BC");
		vipsImpoundObj.setImpoundmentDt(LocalDate.parse("2018-10-22"));
		vipsImpoundment.setResult(vipsImpoundObj);
		
	}
    
    @DisplayName("POST, Success - Impoundment Delegate")
    @Test
    public void testImpoundmentCreateApiPOSTSuccess() {
        
		// Mock underlying VIPS REST service 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse();
		response.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		response.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
		response.setImpoundmentId(123L);
        Mockito.when(service.createImpoundment(createImpoundment)).thenReturn(response);
        
        // Create successful call and validate response 
        ResponseEntity<CreateImpoundmentResponse> controllerResponse = controller.createImpoundment(createImpoundment, null);
        CreateImpoundmentResponse result = controllerResponse.getBody();
        Mockito.verify(service).createImpoundment(createImpoundment);
		Assertions.assertEquals(123L, result.getImpoundmentId());
        Assertions.assertEquals("/impoundments/12345678", controllerResponse.getHeaders().getLocation().toString());
    }
    
    
    @DisplayName("POST, VIPS WS General failure  - Impoundment Delegate")
    @Test
    public void testImpoundmentCreateApiPOSTVipsWSGeneralFailure() {
        
		// Mock underlying VIPS REST service to send VIPS general failure. 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse();
		response.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
		Mockito.when(service.createImpoundment(createImpoundment)).thenReturn(response);
        
		// Ensure Digital Form Exception type is thrown in this case
		assertThrows(DigitalFormsException.class, () -> {
			controller.createImpoundment(createImpoundment, null);
		});
    }
    
    @DisplayName("POST, VIPS JAVA failure  - Impoundment Delegate")
    @Test
    public void testImpoundmentCreateApiPOSTVipsJavaFailure() {
        
		// Mock underlying VIPS REST service to send VIPS general failure. 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse();
		response.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
		Mockito.when(service.createImpoundment(createImpoundment)).thenReturn(response);
        
		// Ensure Digital Form Exception type is thrown in this case
		Assertions.assertThrows(DigitalFormsException.class, () -> {
			controller.createImpoundment(createImpoundment, null);
		});
    }
    
    @DisplayName("POST, BAD REQUEST  - Impoundment Delegate")  
    @Test
	public void testImpoundmentCreateApiPOSTBadRequest() throws Exception {
    	
		String body = UnitTestUtilities.asJsonString(createImpoundment);
		String invalid = body.replace("\"2021-10-08\"", "\"2021-10-22T00:00:00.000-07:00\"");
		Assertions.assertNotEquals(body, invalid);
		
	    mvc.perform( MockMvcRequestBuilders
	    	      .post("/impoundments")
				  .content(invalid)
	    	      .contentType(MediaType.APPLICATION_JSON)
	    	      .accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("impoundment.impoundmentDt"))
				.andExpect(jsonPath("$.errors[0].message").value(TemporalJson.DATE_MESSAGE));
	    
	}
    
    @DisplayName("GET, Search Success  - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETSearchSuccess() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Impoundment response in good case 
		vipsImpoundment.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsImpoundment.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.getImpoundment(impoundmentId)).thenReturn(vipsImpoundment);
        
        // Create successful search notice number call with impoundmentId and validate response 
        ResponseEntity<Impoundment> controllerResponse = controller.getImpoundment(noticeNo, null);
        Impoundment result = controllerResponse.getBody();
        Assertions.assertEquals("BC", result.getDlJurisdictionCd());
        
    }
    
    @DisplayName("GET, Search Not Found  - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETSearchNotFound() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	
    	ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse vipsSearchNotFound = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse();
		List<VipsImpoundmentBasicsObj> results = new ArrayList<VipsImpoundmentBasicsObj>();
		
		//empty response object from VIPS means negative search result. 
		vipsSearchNotFound.setResult(results); 
    	
    	// Mock underlying VIPS REST Search 
		vipsSearchNotFound.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearchNotFound.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearchNotFound);
    	
    	// Ensure ResourceNotFoundException type is thrown in this case
    	Assertions.assertThrows(ResourceNotFoundException.class, () -> {
    			controller.getImpoundment(noticeNo, null);
    	});
    
    }
    
    @DisplayName("GET, Search Result size > 1 - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETSearchResultSizeGreaterThanOne() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    
    	//add one more impoundment ID to the search results.
    	VipsImpoundmentBasicsObj obj2 = new VipsImpoundmentBasicsObj();
		obj2.setImpoundmentId(22222223L); 
		vipsSearch.getResult().add(obj2);
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.getImpoundment(noticeNo, null);
    	});
        

    }
    
    @DisplayName("GET, Search VIPS General Failure - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETSearchVIPSGenFailure() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.getImpoundment(noticeNo, null);
    	});
    	
    	
    }
    
    @DisplayName("GET, Search VIPS Internal Java Failure - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETSearchVIPSInternalFailure() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.getImpoundment(noticeNo, null);
    	});
    	
    	
    }
    
    
    @DisplayName("GET, Impoundment Not Found - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETImpoundmentNotFound() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Impoundment response returning no impoundment even though the
        // search return with an impoundment Id. (Very BAD case for VIPS if this ever happened)
        ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse vipsImpoundmentNone = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse();
        vipsImpoundmentNone.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
 		vipsImpoundmentNone.setResult(null); // no impoundment in the response 
        
		vipsImpoundment.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.getImpoundment(impoundmentId)).thenReturn(vipsImpoundmentNone);
        
        // Ensure ResourceNotFoundException type is thrown in this case
    	Assertions.assertThrows(ResourceNotFoundException.class, () -> {
    			controller.getImpoundment(noticeNo, null);
    	});
    	
    	
    }
    
    @DisplayName("GET, Impoundment General Failure - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETVIPSImpoundmentGenFailure() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Impoundment response reporting general failure 
		vipsImpoundment.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
	    Mockito.when(service.getImpoundment(impoundmentId)).thenReturn(vipsImpoundment);
	    
        // Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.getImpoundment(noticeNo, null);
    	});
    	
    	
    }
    
    @DisplayName("GET, Impoundment VIPS Internal Java Failure - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETVIPSImpoundmentInternalFailure() throws Exception {
    	
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Impoundment response reporting internal Java failure 
		vipsImpoundment.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
	    Mockito.when(service.getImpoundment(impoundmentId)).thenReturn(vipsImpoundment);
	    
        // Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.getImpoundment(noticeNo, null);
    	});
    	
   
    }
    
    @DisplayName("POST, Impoundment unsupported route - Impoundment Delegate")
    @Test
	public void testImpoundmentApiPOSTUnsupportedRoute() throws Exception {
    
		   mvc.perform( MockMvcRequestBuilders
		  	      .post("/impoundments/123456")
		  	      .content(UnitTestUtilities.asJsonString(createImpoundment))
		  	      .contentType(MediaType.APPLICATION_JSON)
		  	      .accept(MediaType.APPLICATION_JSON))
		  	      .andExpect(status().isMethodNotAllowed());
    
		   mvc.perform( MockMvcRequestBuilders
			  	      .post("/impoundments/123456/extra")
			  	      .content(UnitTestUtilities.asJsonString(createImpoundment))
			  	      .contentType(MediaType.APPLICATION_JSON)
			  	      .accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound());
    }
    
    @DisplayName("POST, Impoundment missing body - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiPOSTVIPSImpoundmentNoBody() throws Exception {
    
		   mvc.perform( MockMvcRequestBuilders
				.post("/impoundments")
		  	      .contentType(MediaType.APPLICATION_JSON)
		  	      .accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
    
    }
    
}
