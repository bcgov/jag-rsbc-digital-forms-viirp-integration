package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ca.bc.gov.open.digitalformsapi.viirp.UnitTestUtilities;
import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsAddressCreateObj;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsImpoundObj;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsImpoundmentCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsLicenceCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsRegistrationCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsVehicleCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.VipsImpoundmentBasicsObj;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc(addFilters = false)
public class ImpoundmentsApiControllerTests {
	
	@Autowired
	private MockMvc mvc;
	
	@Mock
    private VipsRestService service;

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
		
		// Mock POST Impoundment body 
		createImpoundment = new CreateImpoundment();
		VipsImpoundmentCreate vipsImpoundmentCreate = new VipsImpoundmentCreate();
		vipsImpoundmentCreate.setDlJurisdictionCd("BC");
		vipsImpoundmentCreate.setImpoundmentDt("2021-10-08T00:00:00.000-07:00");
		vipsImpoundmentCreate.setImpoundmentNoticeNo("12345678");
		vipsImpoundmentCreate.setNoticeSubjectCd("VEHI");
		vipsImpoundmentCreate.setNoticeTypeCd("IMP");
		List<String> originalCauseCds = Arrays.asList("STUNT");
		vipsImpoundmentCreate.setOriginalCauseCds(originalCauseCds);
		vipsImpoundmentCreate.setPoliceDetatchmentId(304L);
		vipsImpoundmentCreate.setPoliceOfficerNm("Jones");
		vipsImpoundmentCreate.setPoliceOfficerNo("420");
		vipsImpoundmentCreate.setPoliceFileNo("29");
		vipsImpoundmentCreate.setSeizureLocationTxt("Victoria");				

		createImpoundment.setVipsImpoundCreate(vipsImpoundmentCreate);	
		
		List<VipsRegistrationCreate> vipsRegistrationCreateArray = new ArrayList<>();
		
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
		
		VipsVehicleCreate vipsVehicleCreate = new VipsVehicleCreate();
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
		
		List<Long> vipsDocumentIdArray = new ArrayList<>();
		
		List<Long> vipsImpoundmentArray = new ArrayList<>();
		
		createImpoundment.setVipsImpoundCreate(vipsImpoundmentCreate); // 1
		createImpoundment.setVipsRegistrationCreateArray(vipsRegistrationCreateArray); // 2
		createImpoundment.setVipsVehicleCreate(vipsVehicleCreate); // 3
		createImpoundment.setVipsDocumentIdArray(vipsDocumentIdArray); // 4
		createImpoundment.setVipsImpoundmentArray(vipsImpoundmentArray); // 5
		
		//Mock VIPS WS Notice number for Impoundment Search response
		vipsSearch = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse();
		List<VipsImpoundmentBasicsObj> results = new ArrayList<VipsImpoundmentBasicsObj>();
		VipsImpoundmentBasicsObj obj = new VipsImpoundmentBasicsObj();
		obj.setImpoundmentId(DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID);
		results.add(obj);
		vipsSearch.setResult(results);
		
		// Mock VIPS WS GET Impoundment response. 
		vipsImpoundment = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse();
		VipsImpoundObj vipsImpoundObj = new VipsImpoundObj();
		vipsImpoundObj.setDlJurisdictionCd("BC");
		vipsImpoundObj.setImpoundmentDt("2018-10-22T00:00:00.000-07:00");
		vipsImpoundment.setResult(vipsImpoundObj);
		
	}
    
    @DisplayName("POST, Success - Impoundment Delegate")
    @Test
    public void testImpoundmentCreateApiPOSTSuccess() {
        
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
		
		// Mock underlying VIPS REST service 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse();
		response.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		response.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.createImpoundment(correlationId, createImpoundment)).thenReturn(response);
        
        // Create successful call and validate response 
        ResponseEntity<CreateImpoundmentServiceResponse> controllerResponse = controller.impoundmentsCorrelationIdPost(correlationId, createImpoundment);
        CreateImpoundmentServiceResponse result = controllerResponse.getBody();
        Mockito.verify(service).createImpoundment(correlationId, createImpoundment);
        Assertions.assertEquals(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG, result.getRespMsg());
    }
    
    
    @DisplayName("POST, VIPS WS General failure  - Impoundment Delegate")
    @Test
    public void testImpoundmentCreateApiPOSTVipsWSGeneralFailure() {
        
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
		
		// Mock underlying VIPS REST service to send VIPS general failure. 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse();
		response.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
		Mockito.when(service.createImpoundment(correlationId, createImpoundment)).thenReturn(response);
        
		// Ensure Digital Form Exception type is thrown in this case
		Assertions.assertThrows(DigitalFormsException.class, () -> {
			controller.impoundmentsCorrelationIdPost(correlationId, createImpoundment);
		});
    }
    
    @DisplayName("POST, VIPS JAVA failure  - Impoundment Delegate")
    @Test
    public void testImpoundmentCreateApiPOSTVipsJavaFailure() {
        
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
		
		// Mock underlying VIPS REST service to send VIPS general failure. 
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse response = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse();
		response.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
		Mockito.when(service.createImpoundment(correlationId, createImpoundment)).thenReturn(response);
        
		// Ensure Digital Form Exception type is thrown in this case
		Assertions.assertThrows(DigitalFormsException.class, () -> {
			controller.impoundmentsCorrelationIdPost(correlationId, createImpoundment);
		});
    }
    
    // TODO - This checks for 500 errors when validation fails. Why - Because of Springboot's inadequate validation handling. 
    // Currently the default 500 error is thrown when validation fails instead of the proper 400 BAD REQUEST. 
    // I don't have an immediate fix for this as it is complicated by the way we presently auto-generate the controller delegates
    // from the specification file.  
    
    @DisplayName("POST, BAD REQUEST  - Impoundment Delegate")  
    @Test
	public void testImpoundmentCreateApiPOSTBadRequest() throws Exception {
    	
    	createImpoundment.getVipsImpoundCreate().setImpoundmentDt("2021-10-22T0a:23:23.100-07:00"); // Doesn;t match regex for date/time. 
		
		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;
	    
	    mvc.perform( MockMvcRequestBuilders
	    	      .post("/impoundments/{correlationId}", correlationId)
	    	      .content(UnitTestUtilities.asJsonString(createImpoundment))
	    	      .contentType(MediaType.APPLICATION_JSON)
	    	      .accept(MediaType.APPLICATION_JSON))
	    	      .andExpect(status().isInternalServerError());
	    
	}
    
    @DisplayName("GET, Search Success  - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETSearchSuccess() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Impoundment response in good case 
		vipsImpoundment.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsImpoundment.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.getImpoundment(correlationId, impoundmentId)).thenReturn(vipsImpoundment);
        
        // Create successful search notice number call with impoundmentId and validate response 
        ResponseEntity<GetImpoundmentServiceResponse> controllerResponse = controller.impoundmentsNoticeNoCorrelationIdGet(noticeNo, correlationId);
        GetImpoundmentServiceResponse result = controllerResponse.getBody();
        Assertions.assertEquals("BC", result.getResult().getDlJurisdictionCd());
        
    }
    
    @DisplayName("GET, Search Not Found  - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETSearchNotFound() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	
    	ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse vipsSearchNotFound = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse();
		List<VipsImpoundmentBasicsObj> results = new ArrayList<VipsImpoundmentBasicsObj>();
		
		//empty response object from VIPS means negative search result. 
		vipsSearchNotFound.setResult(results); 
    	
    	// Mock underlying VIPS REST Search 
		vipsSearchNotFound.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearchNotFound.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(correlationId, noticeNo)).thenReturn(vipsSearchNotFound);
    	
    	// Ensure ResourceNotFoundException type is thrown in this case
    	Assertions.assertThrows(ResourceNotFoundException.class, () -> {
    			controller.impoundmentsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    
    }
    
    @DisplayName("GET, Search Result size > 1 - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETSearchResultSizeGreaterThanOne() throws Exception {
    	
       	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    
    	//add one more impoundment ID to the search results.
    	VipsImpoundmentBasicsObj obj2 = new VipsImpoundmentBasicsObj();
		obj2.setImpoundmentId(22222223L); 
		vipsSearch.getResult().add(obj2);
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.impoundmentsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
        

    }
    
    @DisplayName("GET, Search VIPS General Failure - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETSearchVIPSGenFailure() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
        Mockito.when(service.searchImpoundment(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.impoundmentsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    	
    	
    }
    
    @DisplayName("GET, Search VIPS Internal Java Failure - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETSearchVIPSInternalFailure() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
        Mockito.when(service.searchImpoundment(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.impoundmentsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    	
    	
    }
    
    
    @DisplayName("GET, Impoundment Not Found - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETImpoundmentNotFound() throws Exception {
    	
    	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Impoundment response returning no impoundment even though the
        // search return with an impoundment Id. (Very BAD case for VIPS if this ever happened)
        ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse vipsImpoundmentNone = new ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse();
        vipsImpoundmentNone.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
 		vipsImpoundmentNone.setResult(null); // no impoundment in the response 
        
		vipsImpoundment.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.getImpoundment(correlationId, impoundmentId)).thenReturn(vipsImpoundmentNone);
        
        // Ensure ResourceNotFoundException type is thrown in this case
    	Assertions.assertThrows(ResourceNotFoundException.class, () -> {
    			controller.impoundmentsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    	
    	
    }
    
    @DisplayName("GET, Impoundment General Failure - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETVIPSImpoundmentGenFailure() throws Exception {
    	
      	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Impoundment response reporting general failure 
		vipsImpoundment.setRespCd(DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD);
	    Mockito.when(service.getImpoundment(correlationId, impoundmentId)).thenReturn(vipsImpoundment);
	    
        // Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.impoundmentsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    	
    	
    }
    
    @DisplayName("GET, Impoundment VIPS Internal Java Failure - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETVIPSImpoundmentInternalFailure() throws Exception {
    	
       	String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
    	String noticeNo = DigitalFormsConstants.UNIT_TEST_NOTICE_NUMBER;
    	Long impoundmentId = DigitalFormsConstants.UNIT_TEST_IMPOUNDMENT_ID;
		
		// Mock underlying VIPS REST Search 
		vipsSearch.setRespMsg(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG);
		vipsSearch.setRespCd(DigitalFormsConstants.VIPSWS_SUCCESS_CD);
        Mockito.when(service.searchImpoundment(correlationId, noticeNo)).thenReturn(vipsSearch);
        
        // Mock underlying VIPS REST Get Impoundment response reporting internal Java failure 
		vipsImpoundment.setRespCd(DigitalFormsConstants.VIPSWS_JAVA_EX);
	    Mockito.when(service.getImpoundment(correlationId, impoundmentId)).thenReturn(vipsImpoundment);
	    
        // Ensure DigitalFormsException type is thrown in this case
    	Assertions.assertThrows(DigitalFormsException.class, () -> {
    			controller.impoundmentsNoticeNoCorrelationIdGet(noticeNo, correlationId);
    	});
    	
   
    }
    
    @DisplayName("GET, Impoundment missing CorrelationId or Notice No - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiGETVIPSImpoundmentMissingCorrlelationOrNotice() throws Exception {
    
		   mvc.perform( MockMvcRequestBuilders
		  	      .post("/impoundments")
		  	      .content(UnitTestUtilities.asJsonString(createImpoundment))
		  	      .contentType(MediaType.APPLICATION_JSON)
		  	      .accept(MediaType.APPLICATION_JSON))
		  	      .andExpect(status().isNotFound());
    
		   mvc.perform( MockMvcRequestBuilders
			  	      .post("/impoundments/123456")
			  	      .content(UnitTestUtilities.asJsonString(createImpoundment))
			  	      .contentType(MediaType.APPLICATION_JSON)
			  	      .accept(MediaType.APPLICATION_JSON))
			  	      .andExpect(status().isInternalServerError());
    }
    
    @DisplayName("POST, Impoundment missing body - Impoundment Delegate")  
    @Test
	public void testImpoundmentApiPOSTVIPSImpoundmentNoBody() throws Exception {
    
		   mvc.perform( MockMvcRequestBuilders
		  	      .post("/impoundments/1234567/22222222")
		  	      .contentType(MediaType.APPLICATION_JSON)
		  	      .accept(MediaType.APPLICATION_JSON))
		  	      .andExpect(status().isInternalServerError());
    
    }
    
}
