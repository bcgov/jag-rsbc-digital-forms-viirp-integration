package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.bc.gov.open.digitalformsapi.viirp.api.SearchApi;
 
/**
 * VI IRP Search Controller   
 * 
 * @author 176899
 *
 */

@RestController
public class SearchController implements SearchApi{
 
    @RequestMapping("/test")
    public String hello() {
        return "This is the test RESTful endpoint.";
    }  
}
