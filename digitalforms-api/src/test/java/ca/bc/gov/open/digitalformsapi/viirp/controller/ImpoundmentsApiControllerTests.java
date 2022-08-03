package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsAddressCreateObj;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsImpoundmentCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsLicenceCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsRegistrationCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsVehicleCreate;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ImpoundmentsApiControllerTests {
	
	@Autowired
	private MockMvc mvc;
	
	@Mock
    private VipsRestService service;

    @InjectMocks
    private ImpoundmentsApiDelegateImpl controller;
    
    private CreateImpoundment createImpoundment;
    
    @Autowired
    ConfigProperties properties;
    
    @BeforeEach
	public void init() {
    	
		MockitoAnnotations.openMocks(this);
		
		// Create Mock POST Request
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
        Assert.assertEquals(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG, result.getRespMsg());
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
	    	      .content(asJsonString(createImpoundment))
	    	      .contentType(MediaType.APPLICATION_JSON)
	    	      .accept(MediaType.APPLICATION_JSON))
	    	      .andExpect(status().isInternalServerError());
	    
	}
    
    /**
     * Utility method to convert JSOB object to string. 
     * @param obj
     * @return
     */
    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
