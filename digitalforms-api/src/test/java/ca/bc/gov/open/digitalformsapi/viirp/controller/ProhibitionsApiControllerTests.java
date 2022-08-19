package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ca.bc.gov.open.digitalformsapi.viirp.UnitTestUtilities;
import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsAddressCreateObj;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsLicenceCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsProhibitionCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsRegistrationCreate;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ProhibitionsApiControllerTests {
	
	@Autowired
	private MockMvc mvc;
	
	@Mock
    private VipsRestService service;

    @InjectMocks
    private ProhibitionsApiDelegateImpl controller;
    
    private CreateProhibition createProhibition;
    
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
        Assertions.assertEquals(DigitalFormsConstants.DIGITALFORMS_SUCCESS_MSG, result.getRespMsg());
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
		Assertions.assertThrows(DigitalFormsException.class, () -> {
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
        
		// Ensure Digital Form Exception type is thrown in this case
		Assertions.assertThrows(DigitalFormsException.class, () -> {
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
    
}
