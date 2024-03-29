package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.api.DfPayloadsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.UnauthorizedException;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetDFPayloadServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.PostDFPayloadServiceRequest;
import ca.bc.gov.open.digitalformsapi.viirp.model.PostDFPayloadServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.PutDFPayloadServiceRequest;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DFBooleanUtils;
import ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.handler.ApiException;
import ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.payload.DfPayloadService; 

@Service
public class DfPayloadsApiDelegateImpl implements DfPayloadsApiDelegate {

	private final Logger logger = LoggerFactory.getLogger(DfPayloadsApiDelegateImpl.class);
	
	@Autowired
	private DfPayloadService dfPayloadService;

	@Override
	public ResponseEntity<GetDFPayloadServiceResponse> dfpayloadsNoticeNoCorrelationIdGet(String noticeNo,
			String correlationId) {
		
		logger.info("Heard a call to the endpoint 'dfPayloadsNoticeNoCorrelationIdGet' with noticeNo " + noticeNo + " correlationId: " + correlationId + ".");

		GetDFPayloadServiceResponse resp = new GetDFPayloadServiceResponse(); 
		ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.GetDFPayloadServiceResponse src;
		try {
			 src = dfPayloadService.getDFPayload(noticeNo, correlationId);
		
		} catch (ApiException ex) {
			logger.error("Failure to call DF ORDS, GET DF Payload for notice No: " + noticeNo + " correlationId: " + correlationId + ". Message: " + ex.getMessage() + " ORDS Response Status Cd: " + ex.getCode());

			if (ex.getCode() == HttpStatus.UNAUTHORIZED.value())
				throw new UnauthorizedException(ex.getMessage());
			if (ex.getCode() == HttpStatus.NOT_FOUND.value())
				throw new ResourceNotFoundException(ex.getMessage());
			else 	
				throw new DigitalFormsException(ex.getMessage());
		}
			
		try { 

			// Transfer from ORDS Client Library GetDFPayloadServiceResponse to DF GetDFPayloadServiceResponse type.
			resp.setActive(BooleanUtils.toBoolean(src.getActiveYN()));
			resp.setProcessed(BooleanUtils.toBoolean(src.getProcessedYN()));
			resp.setNoticeTypeCd(src.getNoticeType());
			resp.setPayload((src.getPayload()));
			
		} catch (Exception ex) {
			logger.error("Failure to transfer bean content after DF ORDS, GET DF Payload call for notice No: " + noticeNo + " correlationId: " + correlationId + ". Message: " + ex.getMessage());
			throw new DigitalFormsException(ex.getMessage(), ex);
		}

		return new ResponseEntity<>(resp, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<PostDFPayloadServiceResponse> dfpayloadsNoticeNoCorrelationIdPost(String correlationId,
			String noticeNo, PostDFPayloadServiceRequest postDFPayloadServiceRequest) {

		logger.info("Heard a call to the endpoint 'dfpayloadsNoticeNoCorrelationIdPost' with noticeNo " + noticeNo + " correlationId: " + correlationId + ".");
		
		ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceRequest request = new ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceRequest();
		
		// Transfer the contents of the VI IRP Request into ORDS request model. All required.  
		request.setActiveYN(DFBooleanUtils.getYNFromBoolean(postDFPayloadServiceRequest.getActive()));
		request.setProcessedYN(DFBooleanUtils.getYNFromBoolean(postDFPayloadServiceRequest.getProcessed()));
		request.setNoticeNo(noticeNo);
		request.setNoticeTypeCd(postDFPayloadServiceRequest.getNoticeTypeCd());
		request.setPayload(postDFPayloadServiceRequest.getPayload());
		
		ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse _resp = new ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse(); 
		
		try {
			 _resp = dfPayloadService.postDFPayload(noticeNo, request);
			
		} catch (ApiException ex) {
			logger.error("Failure to call DF ORDS, POST DF Payload for notice No: " + noticeNo + " correlationId: " + correlationId + ". Message: " + ex.getMessage() + " ORDS Response Status Cd: " + ex.getCode());
			
			if (ex.getCode() == HttpStatus.UNAUTHORIZED.value())
				throw new UnauthorizedException(ex.getMessage());
			else
				throw new DigitalFormsException(ex.getMessage());
		}
		
		PostDFPayloadServiceResponse resp = new PostDFPayloadServiceResponse();
		resp.setStatusMessage(_resp.getStatusMessage());

		return new ResponseEntity<>(resp, HttpStatus.OK);

	}
	
	@Override
	public ResponseEntity<PostDFPayloadServiceResponse> dfpayloadsNoticeNoCorrelationIdPut(String correlationId,
			String noticeNo, PutDFPayloadServiceRequest putDFPayloadServiceRequest) {
		
		logger.info("Heard a call to the endpoint 'dfpayloadsNoticeNoCorrelationIdPut' with noticeNo " + noticeNo + " correlationId: " + correlationId + ".");
		
		ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PutDFPayloadServiceRequest request = new ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PutDFPayloadServiceRequest();
		
		// Transfer the contents of the VI IRP Request into ORDS request model. All required.  
		request.setActiveYN(DFBooleanUtils.getYNFromBoolean(putDFPayloadServiceRequest.getActive()));
		request.setProcessedYN(DFBooleanUtils.getYNFromBoolean(putDFPayloadServiceRequest.getProcessed()));
		request.setNoticeTypeCd(putDFPayloadServiceRequest.getNoticeTypeCd());
		request.setPayload(putDFPayloadServiceRequest.getPayload());
				
		ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse _resp = new ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse(); 
		
		try {
			 _resp = dfPayloadService.putDFPayload(noticeNo, correlationId, request);
			
		} catch (ApiException ex) {
			logger.error("Failure to call DF ORDS, PUT DF Payload for notice No: " + noticeNo + " correlationId: " + correlationId + ". Message: " + ex.getMessage() + " ORDS Response Status Cd: " + ex.getCode());
			
			if (ex.getCode() == HttpStatus.UNAUTHORIZED.value())
				throw new UnauthorizedException(ex.getMessage());
			if (ex.getCode() == HttpStatus.NOT_FOUND.value())
				throw new ResourceNotFoundException(ex.getMessage());
			else
				throw new DigitalFormsException(ex.getMessage());
		}

		PostDFPayloadServiceResponse resp = new PostDFPayloadServiceResponse();
		resp.setStatusMessage(_resp.getStatusMessage());
		
		return new ResponseEntity<>(resp, HttpStatus.OK);

	}
	
	@Override
	public ResponseEntity<PostDFPayloadServiceResponse> dfpayloadsNoticeNoCorrelationIdDelete(String correlationId,
			String noticeNo) {

		logger.info("Heard a call to the endpoint 'dfpayloadsNoticeNoCorrelationIdDelete' with noticeNo " + noticeNo);
		
		ca.bc.gov.open.pssg.rsbc.digitalforms.ordsclient.api.model.PostDFPayloadServiceResponse responseFromOrds;
		try {
			// ORDS call to delete DF Payload based on notice no
			responseFromOrds = dfPayloadService.deleteDFPayload(noticeNo, correlationId);	
			
		} catch (ApiException ex) {
			String msg = "ERROR deleting DF Payload from ORDS with noticeNo: " + noticeNo + ". Message: " + ex.getMessage() + " ORDS Response Status Code: " + ex.getCode();
			logger.error(msg, ex);
			
			if (ex.getCode() == HttpStatus.UNAUTHORIZED.value())
				throw new UnauthorizedException(ex.getMessage());
			if (ex.getCode() == HttpStatus.NOT_FOUND.value())
				throw new ResourceNotFoundException(ex.getMessage());
			else
				throw new DigitalFormsException(ex.getMessage());
		}
		
		PostDFPayloadServiceResponse resp = new PostDFPayloadServiceResponse();
		resp.setStatusMessage(responseFromOrds.getStatusMessage());
		
		return new ResponseEntity<>(resp, HttpStatus.OK);
	}
}
