package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
/**
 * This controller for initial testing only. Please remove from project once permanent controllers are added.  
 * 
 * @author 176899
 *
 */

@RestController
public class TestController {
 
    @RequestMapping("/test")
    public String hello() {
        return "This is the test RESTful endpoint.v2.33";
    }  
}
