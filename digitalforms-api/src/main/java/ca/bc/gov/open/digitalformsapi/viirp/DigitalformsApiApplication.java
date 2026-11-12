package ca.bc.gov.open.digitalformsapi.viirp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ca.bc.gov.open.digitalformsapi", "ca.bc.gov.open.jagvipsclient", "ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient"})
public class DigitalformsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigitalformsApiApplication.class, args);
	}

}
