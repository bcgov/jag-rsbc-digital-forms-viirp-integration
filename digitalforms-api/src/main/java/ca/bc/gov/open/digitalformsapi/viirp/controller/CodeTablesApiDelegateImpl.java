package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientException;

import ca.bc.gov.open.digitalformsapi.viirp.api.CodetablesApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;

@Service
public class CodeTablesApiDelegateImpl implements CodetablesApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(CodeTablesApiDelegateImpl.class);
	
	@Autowired
	private VipsRestService digitalformsApiService;
	
	@Override
	public ResponseEntity<GetCodetablesServiceResponse> getCodeTables(String xCorrelationID) {
		
		logger.info("Heard a call to the endpoint 'getCodeTables'");
		
		GetCodetablesServiceResponse _resp = new GetCodetablesServiceResponse();
		
		try {
			_resp = digitalformsApiService.getCodeTableValues();
		} catch (WebClientException e) {
			logger.error("VIPS Internal Server Error: " + e.getMessage());
			throw new DigitalFormsException("Internal Server Error at VIPS WS. Failed to get codetables.");
		}
		
		if (_resp == null || _resp.getConfiguration() == null) {
			throw new DigitalFormsException("VIPS returned no code-table configuration.");
		}
		return new ResponseEntity<GetCodetablesServiceResponse>(_resp, HttpStatus.OK);
	}
}
