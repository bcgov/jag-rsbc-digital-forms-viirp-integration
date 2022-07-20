package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ForbiddenException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.UnauthorizedException;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

/**
 * 
 * Used only to test ControllerExceptionHandler. 
 * 
 * @see: ca.bc.gov.open.digitalformsapi.viirp.exception.ControllerExceptionHandlerTests
 * 
 * @author 176899
 *
 */

@Controller
public class ExceptionTestController {

	@GetMapping("/exception/{exception_id}")
	public void getSpecificException(@PathVariable("exception_id") String pException) {
		
		// Generic Digital forms Exception type. 
	    if(DigitalFormsConstants.EXCEPTION_DIGITAL_FORMS_TEST.equals(pException)) {
	    	throw new DigitalFormsException(DigitalFormsConstants.EXCEPTION_DIGITAL_FORMS_TEST);
	    }
	    else if(DigitalFormsConstants.EXCEPTION_NOT_FOUND.equals(pException)) {
	        throw new ResourceNotFoundException(DigitalFormsConstants.EXCEPTION_NOT_FOUND);
	    }
	    else if(DigitalFormsConstants.EXCEPTION_FORBIDDEN.equals(pException)) {
	        throw new ForbiddenException(DigitalFormsConstants.EXCEPTION_FORBIDDEN);
	    }
	    else if(DigitalFormsConstants.EXCEPTION_UNAUTHORIZED.equals(pException)) {
	        throw new UnauthorizedException(DigitalFormsConstants.EXCEPTION_UNAUTHORIZED);
	    }
	}
}
