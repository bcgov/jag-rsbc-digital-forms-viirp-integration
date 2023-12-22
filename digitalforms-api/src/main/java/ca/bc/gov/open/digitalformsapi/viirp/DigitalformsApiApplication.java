package ca.bc.gov.open.digitalformsapi.viirp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ca.bc.gov.open.digitalformsapi.viirp", "ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient","ca.bc.gov.open.jag.ordsvipsclient", "ca.bc.gov.open.jagvipsclient"})
public class DigitalformsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigitalformsApiApplication.class, args);
	}

}
