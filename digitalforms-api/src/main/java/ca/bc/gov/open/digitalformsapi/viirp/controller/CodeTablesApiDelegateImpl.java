package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.ConfigurationApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetConfigurationServiceResponse;

@Service
public class ConfigurationApiDelegateImpl implements ConfigurationApiDelegate{
	
	@Override
	public ResponseEntity<GetConfigurationServiceResponse> configurationCorrelationIdGet(String correlationId) {
		
		System.out.println("Heard a call to the endpoint 'configurationCorrelationIdGet' with correlationId " + correlationId);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
