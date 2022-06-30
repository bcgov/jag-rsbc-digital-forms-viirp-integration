package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.SearchApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.model.SearchNoticeNumberServiceResponse;

@Service
public class SearchApiDelegateImpl implements SearchApiDelegate {
	
	@Override
	public ResponseEntity<SearchNoticeNumberServiceResponse> searchCorrelationIdGet(String correlationId,
	        String noticeNo) {
		
		 System.out.println("Heard a call to search with correlationId " + correlationId + " and noticeNo : " + noticeNo);
		
		 return new ResponseEntity<>(HttpStatus.OK);
	}
}
