package ca.bc.gov.open.digitalformsapi.viirp.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.pdfbox.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import ca.bc.gov.open.digitalformsapi.viirp.api.DocumentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.BadRequestException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetDocumentsListServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsDocumentObj;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsDocumentResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.VipsNoticeObj;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import ca.bc.gov.open.jag.ordsvipsclient.api.DocumentApi;
import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiException;
import ca.bc.gov.open.jag.ordsvipsclient.api.model.VipsDocumentOrdsResponse;

@Service
public class DocumentsApiDelegateImpl implements DocumentsApiDelegate{

	private static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;
	// VIPS_DOCUMENTS.PAGE_COUNT_QTY is NUMBER(5).
	private static final int MAX_DOCUMENT_PAGES = 99_999;

	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	private DocumentApi documentApi;
	
	@Autowired
	private  ConfigProperties properties;
	
	private final Logger logger = LoggerFactory.getLogger(DocumentsApiDelegateImpl.class);
	
	@Autowired
	private VipsRestService digitalformsApiService;
	
	@Override
	public ResponseEntity<GetDocumentsListServiceResponse> listProhibitionDocuments(
	        String noticeNo, String xCorrelationID) {
		Long prohibitionId = findProhibitionId(noticeNo);
		return getNoticeDocuments(noticeNo, null, prohibitionId);
	}

	@Override
	public ResponseEntity<GetDocumentsListServiceResponse> listImpoundmentDocuments(
	        String noticeNo, String xCorrelationID) {
		Long impoundmentId = findImpoundmentId(noticeNo);
		return getNoticeDocuments(noticeNo, impoundmentId, null);
	}

	@Override
	public ResponseEntity<Void> associateDocumentToProhibition(
	        String noticeNo, Long documentId, String xCorrelationID) {
		Long prohibitionId = findProhibitionId(noticeNo);
		return associateDocument(noticeNo, documentId, null, prohibitionId);
	}

	@Override
	public ResponseEntity<Void> associateDocumentToImpoundment(
	        String noticeNo, Long documentId, String xCorrelationID) {
		Long impoundmentId = findImpoundmentId(noticeNo);
		return associateDocument(noticeNo, documentId, impoundmentId, null);
	}

	private Long findImpoundmentId(String noticeNo) {
		SearchImpoundmentsServiceResponse search;
		try {
			search = digitalformsApiService.searchImpoundment(noticeNo);
		} catch (WebClientException e) {
			throw new DigitalFormsException("Failed to search for impoundment notice " + noticeNo, e);
		}
		if (search == null || search.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			throw new DigitalFormsException("VIPS impoundment search failed for notice " + noticeNo);
		}
		if (search.getResult() == null || search.getResult().isEmpty()) {
			throw new ResourceNotFoundException("Impoundment notice " + noticeNo + " not found.");
		}
		if (search.getResult().size() != 1 || search.getResult().get(0).getImpoundmentId() == null) {
			throw new DigitalFormsException("VIPS returned an invalid impoundment search result for notice " + noticeNo);
		}
		return search.getResult().get(0).getImpoundmentId();
	}

	private Long findProhibitionId(String noticeNo) {
		SearchProhibitionsServiceResponse search;
		try {
			search = digitalformsApiService.searchProhibition(noticeNo);
		} catch (WebClientException e) {
			throw new DigitalFormsException("Failed to search for prohibition notice " + noticeNo, e);
		}
		if (search == null || search.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			throw new DigitalFormsException("VIPS prohibition search failed for notice " + noticeNo);
		}
		if (search.getResult() == null || search.getResult().isEmpty()) {
			throw new ResourceNotFoundException("Prohibition notice " + noticeNo + " not found.");
		}
		if (search.getResult().size() != 1 || search.getResult().get(0).getProhibitionId() == null) {
			throw new DigitalFormsException("VIPS returned an invalid prohibition search result for notice " + noticeNo);
		}
		return search.getResult().get(0).getProhibitionId();
	}

	private ResponseEntity<GetDocumentsListServiceResponse> getNoticeDocuments(
	        String noticeNo, Long impoundmentId, Long prohibitionId) {
		ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetDocumentsListServiceResponse upstream;
		try {
			upstream = digitalformsApiService.getDocumentsMetaList(impoundmentId, prohibitionId);
		} catch (WebClientException e) {
			throw new DigitalFormsException("Failed to get documents for notice " + noticeNo, e);
		}
		if (upstream == null || upstream.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			logger.error("VIPS document lookup failed for notice {}", noticeNo);
			throw new DigitalFormsException("Failed to get documents for notice " + noticeNo);
		}
		GetDocumentsListServiceResponse response = new GetDocumentsListServiceResponse();
		response.setResults(upstream.getResults() == null ? List.of() : upstream.getResults());
		return ResponseEntity.ok(response);
	}

	private ResponseEntity<Void> associateDocument(
	        String noticeNo, Long documentId, Long impoundmentId, Long prohibitionId) {
		if (containsDocument(getNoticeDocuments(noticeNo, impoundmentId, prohibitionId).getBody(), documentId)) {
			return ResponseEntity.noContent().build();
		}

		String noticeTypeCd = impoundmentId != null
				? findImpoundmentNoticeType(impoundmentId)
				: findProhibitionNoticeType(prohibitionId);
		VipsNoticeObj notice = new VipsNoticeObj();
		notice.setNoticeNo(noticeNo);
		notice.setNoticeTypeCd(noticeTypeCd);

		ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse result;
		try {
			result = digitalformsApiService.createDocumentAsociationPost(documentId, notice);
		} catch (WebClientException e) {
			throw new DigitalFormsException("Failed to associate document " + documentId
					+ " with notice " + noticeNo, e);
		}
		if (result == null) {
			throw new DigitalFormsException("VIPS returned an empty document-association response.");
		}
		if (result.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			return ResponseEntity.noContent().build();
		}
		if (result.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD
				&& DigitalFormsConstants.VIPS_NOTICE_NOT_FOUND.equalsIgnoreCase(result.getRespMsg())) {
			throw new ResourceNotFoundException("Notice " + noticeNo + " not found.");
		}
		if (containsDocument(getNoticeDocuments(noticeNo, impoundmentId, prohibitionId).getBody(), documentId)) {
			return ResponseEntity.noContent().build();
		}
		logger.error("VIPS document association failed with response code {} and message {}",
				result.getRespCd(), result.getRespMsg());
		throw new DigitalFormsException("Failed to associate document " + documentId
				+ " with notice " + noticeNo);
	}

	private String findImpoundmentNoticeType(Long impoundmentId) {
		GetImpoundmentServiceResponse response;
		try {
			response = digitalformsApiService.getImpoundment(impoundmentId);
		} catch (WebClientException e) {
			throw new DigitalFormsException("Failed to retrieve impoundment " + impoundmentId, e);
		}
		if (response == null || response.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD
				|| response.getResult() == null || response.getResult().getNoticeTypeCd() == null
				|| response.getResult().getNoticeTypeCd().isBlank()) {
			throw new DigitalFormsException("VIPS returned no notice type for impoundment " + impoundmentId);
		}
		return response.getResult().getNoticeTypeCd();
	}

	private String findProhibitionNoticeType(Long prohibitionId) {
		GetProhibitionServiceResponse response;
		try {
			response = digitalformsApiService.getProhibition(prohibitionId);
		} catch (WebClientException e) {
			throw new DigitalFormsException("Failed to retrieve prohibition " + prohibitionId, e);
		}
		if (response == null || response.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD
				|| response.getResult() == null || response.getResult().getNoticeTypeCd() == null
				|| response.getResult().getNoticeTypeCd().isBlank()) {
			throw new DigitalFormsException("VIPS returned no notice type for prohibition " + prohibitionId);
		}
		return response.getResult().getNoticeTypeCd();
	}

	private boolean containsDocument(GetDocumentsListServiceResponse documents, Long documentId) {
		return documents != null && documents.getResults() != null
				&& documents.getResults().stream()
						.filter(Objects::nonNull)
						.anyMatch(document -> Objects.equals(documentId, document.getDocumentId()));
	}
	
	/**
	 * POST Document (Store document in VIPS) 
	 * 
	 * Possible Response codes:
	 * 	201. created. 
	 * 	400. invalid upload (not exactly one readable PDF with 1 to 99,999 pages, or invalid metadata)
	 * 	401. unauth (digital forms basic auth failure)
	 * 	500. from the SSG if VIPS WS is unavailable or responds with any code other than 200. 
	 * 
	 * The page count stored in VIPS is read from the PDF, not supplied by the client. 
	 * @throws ApiException 
	 */
	@Override
	public ResponseEntity<VipsDocumentResponse> storeDocument(
	        String typeCode, MultipartFile file, String xCorrelationID,
	        String noticeTypeCode, String noticeSubjectCode) {
		
		logger.info("Heard a call to the endpoint 'storeDocument'");
		
		PdfUpload upload = readPdfUpload(file);
		VipsDocumentResponse resp = new VipsDocumentResponse();
	
		VipsDocumentOrdsResponse _resp;
		try {
			_resp = documentApi.storeDocumentPost(
						typeCode, "application", "pdf",
						properties.getVipsRestApiCredentialsGuid(),
						upload.content(), noticeTypeCode, noticeSubjectCode, upload.pageCount());
			
			// Depending on the result code from the VIPS store document call, set the response entity accordingly. 
			if (_resp.getStatusCode().equals(DigitalFormsConstants.VIPSORDS_SUCCESS_CD)) {
				if (_resp.getDocumentId() == null || _resp.getDocumentId().isBlank()) {
					throw new DigitalFormsException("VIPS ORDS returned success without a document ID.");
				}
				resp.setDocumentId(_resp.getDocumentId());
			} else if (_resp.getStatusCode().equals(DigitalFormsConstants.VIPSORDS_GENERAL_FAILURE_CD)) {
				logger.error("VIPS error response code: " + _resp.getStatusCode() + ", response msg:  " + _resp.getStatusMessage());
				throw new DigitalFormsException("Failed to store document in VIPS ORDS. Type Code: " + typeCode);
			} else {
				throw new DigitalFormsException("Unexpected VIPS ORDS status: " + _resp.getStatusCode());
			}
			
			return new ResponseEntity<>(resp, HttpStatus.CREATED);
			
			
		} catch (ApiException e) {
			throw new DigitalFormsException("VIPS ORDS document upload failed.", e);
		}			 
		
	}

	private record PdfUpload(byte[] content, int pageCount) { }

	private PdfUpload readPdfUpload(MultipartFile file) {
		byte[] content;
		try {
			// MultipartFile binding alone would silently select the first repeated file part.
			long fileParts = request.getParts().stream()
					.filter(part -> "file".equals(part.getName()) || part.getSubmittedFileName() != null)
					.count();
			if (fileParts != 1) {
				throw new BadRequestException("file", "Exactly one file part named 'file' is required.");
			}
			if (file.isEmpty()) {
				throw new BadRequestException("file", "file must not be empty.");
			}
			if (file.getSize() > MAX_DOCUMENT_BYTES) {
				throw new MaxUploadSizeExceededException(MAX_DOCUMENT_BYTES);
			}
			content = file.getBytes();
		} catch (ServletException e) {
			throw new BadRequestException("Malformed multipart request.");
		} catch (IOException e) {
			throw new DigitalFormsException("Unable to read the uploaded file.", e);
		}

		if (content.length < 5 || !new String(content, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
			throw new BadRequestException("file", "file must contain a readable PDF document.");
		}
		int pageCount;
		try (var document = Loader.loadPDF(content)) {
			pageCount = document.getNumberOfPages();
		} catch (IOException e) {
			logger.warn("PDF upload validation failed: {}", e.getMessage());
			throw new BadRequestException("file", "file must contain a readable PDF that does not require a password.");
		}
		if (pageCount < 1) {
			throw new BadRequestException("file", "file must contain a PDF with at least one page.");
		}
		if (pageCount > MAX_DOCUMENT_PAGES) {
			throw new BadRequestException("file", "file must contain a PDF with at most " + MAX_DOCUMENT_PAGES + " pages.");
		}
		return new PdfUpload(content, pageCount);
	}
	
	@Override
	public ResponseEntity<Resource> getDocument(
	        Long documentId,
	        String xCorrelationID) {
		
		logger.info("Heard a call to the endpoint 'getDocument' with documentId " + documentId);
		
		ResponseEntity<Flux<DataBuffer>> documentResponse;
		try {
			documentResponse = digitalformsApiService.getDocument(documentId);
		} catch (WebClientResponseException.NotFound e) {
			throw new ResourceNotFoundException("Document Not Found");
		} catch (WebClientException e) {
			logger.error("VIPS Internal Server Error: " + e.getMessage());
			throw new DigitalFormsException("Internal Server Error at VIPS WS. Failed to retrieve document " + documentId, e);
		}
		if (documentResponse == null || documentResponse.getBody() == null) {
			throw new DigitalFormsException("VIPS returned an empty document response for document " + documentId);
		}

		InputStream stream = DataBufferUtils.subscriberInputStream(documentResponse.getBody(), 1);
		int firstByte;
		try {
			firstByte = stream.read();
		} catch (IOException e) {
			try {
				stream.close();
			} catch (IOException closeException) {
				e.addSuppressed(closeException);
			}
			throw new DigitalFormsException("Unable to read document " + documentId + " from VIPS.", e);
		}
		if (firstByte < 0) {
			try {
				stream.close();
			} catch (IOException e) {
				throw new DigitalFormsException("Unable to close empty document stream for " + documentId, e);
			}
			throw new ResourceNotFoundException("Document Not Found");
		}

		long contentLength = documentResponse.getHeaders().getContentLength();
		InputStreamResource pdf = new InputStreamResource(new SequenceInputStream(
				new ByteArrayInputStream(new byte[] {(byte) firstByte}), stream)) {
			@Override
			public long contentLength() {
				return contentLength;
			}

			@Override
			public String getFilename() {
				return "document-" + documentId + ".pdf";
			}
		};
		ResponseEntity.BodyBuilder response = ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"document-" + documentId + ".pdf\"")
				.header(HttpHeaders.CACHE_CONTROL, "no-store");
		if (contentLength >= 0) {
			response.contentLength(contentLength);
		}
		return response.body(pdf);
	}

}
