package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.bc.gov.open.digitalforms.api.SearchApi;
import ca.bc.gov.open.digitalforms.model.SearchNoticeNumberServiceResponse;
 
/**
 * This controller for initial testing only. Please remove from project once permanent controllers are added.  
 * 
 * @author 176899
 *
 */

@RestController
public class TestController implements SearchApi {
 
    @RequestMapping("/test")
    public String hello() {
        return "This is the test RESTful endpoint.";
    }
    
    @Override
    public ResponseEntity<SearchNoticeNumberServiceResponse> apiV1SearchCorrelationIdGet(String correlationId, String noticeNo){
    	return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
