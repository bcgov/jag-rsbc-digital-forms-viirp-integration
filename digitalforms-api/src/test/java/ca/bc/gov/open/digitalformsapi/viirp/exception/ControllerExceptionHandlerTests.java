package ca.bc.gov.open.digitalformsapi.viirp.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ControllerExceptionHandlerTests {
	
	@Autowired
	private MockMvc mvc;

	@Test
	void controllerExceptionHandler_NotFound_test() throws Exception {
	    
		String exceptionParam = DigitalFormsConstants.EXCEPTION_NOT_FOUND;

	    mvc.perform(get("/exception/{exception_id}", exceptionParam)
	      .contentType(MediaType.APPLICATION_JSON))
	      .andExpect(status().isNotFound())
	      .andExpect(content().json("{'status_message':'Not Found'}"));
	}
	
	@Test
	void controllerExceptionHandler_Forbidden_test() throws Exception {
	    
		String exceptionParam = DigitalFormsConstants.EXCEPTION_FORBIDDEN;

	    mvc.perform(get("/exception/{exception_id}", exceptionParam)
	      .contentType(MediaType.APPLICATION_JSON))
	      .andExpect(status().isForbidden())
	      .andExpect(content().json("{'status_message':'Forbidden'}"));
	}
	
	@Test
	void controllerExceptionHandler_Unauthorized_test() throws Exception {
	    
		String exceptionParam = DigitalFormsConstants.EXCEPTION_UNAUTHORIZED;

	    mvc.perform(get("/exception/{exception_id}", exceptionParam)
	      .contentType(MediaType.APPLICATION_JSON))
	      .andExpect(status().isUnauthorized())
	      .andExpect(content().json("{'status_message':'Unauthorized'}"));
	}
	
	@Test
	void controllerExceptionHandler_Digital_Forms__test() throws Exception {
	    
		String exceptionParam = DigitalFormsConstants.EXCEPTION_DIGITAL_FORMS_TEST;

	    mvc.perform(get("/exception/{exception_id}", exceptionParam)
	      .contentType(MediaType.APPLICATION_JSON))
	      .andExpect(status().isInternalServerError())
	      .andExpect(content().json("{'status_message':'Digital Forms Error'}"));
	}
	
}
