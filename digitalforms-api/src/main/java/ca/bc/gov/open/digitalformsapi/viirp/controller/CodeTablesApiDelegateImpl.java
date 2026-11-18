package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.CodetablesApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.UpstreamServiceException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CodeTablesResponse;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;

@Service
public class CodeTablesApiDelegateImpl implements CodetablesApiDelegate{
	
	private final Logger logger = LoggerFactory.getLogger(CodeTablesApiDelegateImpl.class);
	
	@Autowired
	private VipsRestService digitalformsApiService;
	
	@Override
	public ResponseEntity<CodeTablesResponse> getCodeTables(String xCorrelationID) {
		
		logger.info("Heard a call to the endpoint 'getCodeTables'");
		
		CodeTablesResponse _resp = digitalformsApiService.getCodeTableValues();
		
		if (_resp == null || _resp.getConfiguration() == null) {
			throw UpstreamServiceException.invalidResponse("VIPS returned no code-table configuration.");
		}
		return new ResponseEntity<CodeTablesResponse>(_resp, HttpStatus.OK);
	}
}
