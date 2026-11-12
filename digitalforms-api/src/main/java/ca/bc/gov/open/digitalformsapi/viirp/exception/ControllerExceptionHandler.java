package ca.bc.gov.open.digitalformsapi.viirp.exception;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import ca.bc.gov.open.digitalformsapi.viirp.model.ErrorMessage;

/**
 * 
 * Global RESTful Controller Exception handler
 *  
 * @author 176899
 *
 */
@RestControllerAdvice
public class ControllerExceptionHandler {
	
  private final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);	
	
  @ExceptionHandler(DigitalFormsException.class)
  @ResponseBody
  public ResponseEntity<ErrorMessage> digitalFormsException(DigitalFormsException ex, WebRequest request) {
	  
	logger.error(ex.getMessage()); 
	  
    ErrorMessage message = new ErrorMessage();
    message.setStatusMessage(ex.getMessage());
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    
    return new ResponseEntity<>(message, headers, HttpStatus.INTERNAL_SERVER_ERROR);
  }
  
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorMessage> resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
    ErrorMessage message = new ErrorMessage();
    message.setStatusMessage(ex.getMessage());
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    
    return new ResponseEntity<ErrorMessage>(message, headers, HttpStatus.NOT_FOUND);
  }
  
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorMessage> unauthorizedException(UnauthorizedException ex, WebRequest request) {
    ErrorMessage message = new ErrorMessage();
    message.setStatusMessage(ex.getMessage());
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    
    return new ResponseEntity<ErrorMessage>(message, headers, HttpStatus.UNAUTHORIZED);
  }
  
  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorMessage> forbiddenException(ForbiddenException ex, WebRequest request) {
    ErrorMessage message = new ErrorMessage();
    message.setStatusMessage(ex.getMessage());
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    
    return new ResponseEntity<ErrorMessage>(message, headers, HttpStatus.FORBIDDEN);
  }
  
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorMessage> globalExceptionHandler(Exception ex, WebRequest request) {
	    ErrorMessage message = new ErrorMessage();
	    message.setStatusMessage(ex.getMessage());
	    
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
    
    return new ResponseEntity<ErrorMessage>(message, headers, HttpStatus.INTERNAL_SERVER_ERROR);
  }
  
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorMessage> badRequestExceptionHandler(Exception ex, WebRequest request) {
	    ErrorMessage message = new ErrorMessage();
	    message.setStatusMessage(ex.getMessage());
	    
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
    
    return new ResponseEntity<ErrorMessage>(message, headers, HttpStatus.BAD_REQUEST);
  }
}