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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import ca.bc.gov.open.digitalformsapi.viirp.api.DocumentsApiDelegate;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.BadRequestException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.UpstreamServiceException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateDocumentResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.Document;
import ca.bc.gov.open.digitalformsapi.viirp.model.UploadNoticeSubject;
import ca.bc.gov.open.digitalformsapi.viirp.model.UploadNoticeType;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.VipsNoticeObj;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsRestService;
import ca.bc.gov.open.digitalformsapi.viirp.service.VipsOrdsService;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import ca.bc.gov.open.jag.ordsvipsclient.api.model.VipsDocumentOrdsResponse;

@Service
public class DocumentsApiDelegateImpl implements DocumentsApiDelegate{

	private static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;
	// VIPS_DOCUMENTS.PAGE_COUNT_QTY is NUMBER(5).
	private static final int MAX_DOCUMENT_PAGES = 99_999;

	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	private VipsOrdsService vipsOrdsService;
	
	private final Logger logger = LoggerFactory.getLogger(DocumentsApiDelegateImpl.class);
	
	@Autowired
	private VipsRestService digitalformsApiService;
	
	@Override
	public ResponseEntity<List<Document>> listProhibitionDocuments(
	        String noticeNo, String xCorrelationID) {
		Long prohibitionId = findProhibitionId(noticeNo);
		return ResponseEntity.ok(getNoticeDocuments(noticeNo, null, prohibitionId));
	}

	@Override
	public ResponseEntity<List<Document>> listImpoundmentDocuments(
	        String noticeNo, String xCorrelationID) {
		Long impoundmentId = findImpoundmentId(noticeNo);
		return ResponseEntity.ok(getNoticeDocuments(noticeNo, impoundmentId, null));
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
		SearchImpoundmentsServiceResponse search = digitalformsApiService.searchImpoundment(noticeNo);
		if (search == null || search.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			throw UpstreamServiceException.invalidResponse("VIPS impoundment search failed for notice " + noticeNo);
		}
		if (search.getResult() == null || search.getResult().isEmpty()) {
			throw new ResourceNotFoundException("Impoundment notice " + noticeNo + " not found.");
		}
		if (search.getResult().size() != 1 || search.getResult().getFirst() == null
				|| search.getResult().getFirst().getImpoundmentId() == null) {
			throw UpstreamServiceException.invalidResponse("VIPS returned an invalid impoundment search result for notice " + noticeNo);
		}
		return search.getResult().get(0).getImpoundmentId();
	}

	private Long findProhibitionId(String noticeNo) {
		SearchProhibitionsServiceResponse search = digitalformsApiService.searchProhibition(noticeNo);
		if (search == null || search.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			throw UpstreamServiceException.invalidResponse("VIPS prohibition search failed for notice " + noticeNo);
		}
		if (search.getResult() == null || search.getResult().isEmpty()) {
			throw new ResourceNotFoundException("Prohibition notice " + noticeNo + " not found.");
		}
		if (search.getResult().size() != 1 || search.getResult().getFirst() == null
				|| search.getResult().getFirst().getProhibitionId() == null) {
			throw UpstreamServiceException.invalidResponse("VIPS returned an invalid prohibition search result for notice " + noticeNo);
		}
		return search.getResult().get(0).getProhibitionId();
	}

	private List<Document> getNoticeDocuments(
	        String noticeNo, Long impoundmentId, Long prohibitionId) {
		var upstream = digitalformsApiService.getDocumentsMetaList(impoundmentId, prohibitionId);
		if (upstream == null || upstream.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			logger.error("VIPS document lookup failed for notice {}", noticeNo);
			throw UpstreamServiceException.invalidResponse("Failed to get documents for notice " + noticeNo);
		}
		return upstream.getResults() == null ? List.of() : upstream.getResults();
	}

	private ResponseEntity<Void> associateDocument(
	        String noticeNo, Long documentId, Long impoundmentId, Long prohibitionId) {
		if (containsDocument(getNoticeDocuments(noticeNo, impoundmentId, prohibitionId), documentId)) {
			return ResponseEntity.noContent().build();
		}

		String noticeTypeCd = impoundmentId != null
				? findImpoundmentNoticeType(impoundmentId)
				: findProhibitionNoticeType(prohibitionId);
		VipsNoticeObj notice = new VipsNoticeObj();
		notice.setNoticeNo(noticeNo);
		notice.setNoticeTypeCd(noticeTypeCd);

		var result = digitalformsApiService.createDocumentAsociationPost(documentId, notice);
		if (result == null) {
			throw UpstreamServiceException.invalidResponse("VIPS returned an empty document-association response.");
		}
		if (result.getRespCd() == DigitalFormsConstants.VIPSWS_SUCCESS_CD) {
			return ResponseEntity.noContent().build();
		}
		if (result.getRespCd() == DigitalFormsConstants.VIPSWS_GENERAL_FAILURE_CD
				&& DigitalFormsConstants.VIPS_NOTICE_NOT_FOUND.equalsIgnoreCase(result.getRespMsg())) {
			throw new ResourceNotFoundException("Notice " + noticeNo + " not found.");
		}
		if (containsDocument(getNoticeDocuments(noticeNo, impoundmentId, prohibitionId), documentId)) {
			return ResponseEntity.noContent().build();
		}
		logger.error("VIPS document association failed with response code {} and message {}",
				result.getRespCd(), result.getRespMsg());
		throw UpstreamServiceException.invalidResponse("Failed to associate document " + documentId
				+ " with notice " + noticeNo);
	}

	private String findImpoundmentNoticeType(Long impoundmentId) {
		GetImpoundmentServiceResponse response = digitalformsApiService.getImpoundment(impoundmentId);
		if (response == null || response.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD
				|| response.getResult() == null || response.getResult().getNoticeTypeCd() == null
				|| response.getResult().getNoticeTypeCd().isBlank()) {
			throw UpstreamServiceException.invalidResponse("VIPS returned no notice type for impoundment " + impoundmentId);
		}
		return response.getResult().getNoticeTypeCd();
	}

	private String findProhibitionNoticeType(Long prohibitionId) {
		GetProhibitionServiceResponse response = digitalformsApiService.getProhibition(prohibitionId);
		if (response == null || response.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD
				|| response.getResult() == null || response.getResult().getNoticeTypeCd() == null
				|| response.getResult().getNoticeTypeCd().isBlank()) {
			throw UpstreamServiceException.invalidResponse("VIPS returned no notice type for prohibition " + prohibitionId);
		}
		return response.getResult().getNoticeTypeCd();
	}

	private boolean containsDocument(List<Document> documents, Long documentId) {
		return documents.stream().filter(Objects::nonNull)
				.anyMatch(document -> Objects.equals(documentId, document.getDocumentId()));
	}
	
	/**
	 * POST Document (Store document in VIPS) 
	 * 
	 * Possible Response codes:
	 * 	201. created. 
	 * 	400. invalid upload (not exactly one readable PDF with 1 to 99,999 pages, or invalid metadata)
	 * 	401. invalid or missing bearer token
	 * 	502/503/504. VIPS response failure, unavailability or timeout.
	 * 	500. local VI/IRP processing failure.
	 * 
	 * The page count stored in VIPS is read from the PDF, not supplied by the client. 
	 */
	@Override
	public ResponseEntity<CreateDocumentResponse> storeDocument(
	        String documentTypeCd, MultipartFile file, String xCorrelationID,
	        UploadNoticeType noticeTypeCd, UploadNoticeSubject noticeSubjectCd) {
		
		logger.info("Heard a call to the endpoint 'storeDocument'");
		
		PdfUpload upload = readPdfUpload(file);
		CreateDocumentResponse resp = new CreateDocumentResponse();
	
		VipsDocumentOrdsResponse _resp = vipsOrdsService.storeDocument(
				documentTypeCd, upload.content(), noticeTypeCd == null ? null : noticeTypeCd.getValue(),
				noticeSubjectCd == null ? null : noticeSubjectCd.getValue(), upload.pageCount());
		if (_resp == null) {
			throw UpstreamServiceException.invalidResponse("VIPS ORDS returned an empty document-upload response.");
		}
		// Depending on the result code from the VIPS store document call, set the response entity accordingly.
		if (DigitalFormsConstants.VIPSORDS_SUCCESS_CD.equals(_resp.getStatusCode())) {
			if (_resp.getDocumentId() == null || !_resp.getDocumentId().matches("[0-9]+")) {
				throw UpstreamServiceException.invalidResponse("VIPS ORDS returned an invalid document ID.");
			}
			long documentId;
			try {
				documentId = Long.parseLong(_resp.getDocumentId());
			} catch (NumberFormatException e) {
				throw UpstreamServiceException.invalidResponse("VIPS ORDS returned a document ID outside the int64 range.");
			}
			if (documentId < 1) {
				throw UpstreamServiceException.invalidResponse("VIPS ORDS returned a nonpositive document ID.");
			}
			resp.setDocumentId(documentId);
		} else {
			logger.error("VIPS ORDS error response code: {}, response msg: {}", _resp.getStatusCode(), _resp.getStatusMessage());
			throw UpstreamServiceException.invalidResponse("VIPS ORDS document upload failed. Document type code: " + documentTypeCd);
		}
		return ResponseEntity.created(UriComponentsBuilder.fromPath(request.getContextPath())
				.pathSegment("documents", String.valueOf(resp.getDocumentId())).build().encode().toUri()).body(resp);
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
		
		ResponseEntity<Flux<DataBuffer>> documentResponse = digitalformsApiService.getDocument(documentId);
		if (documentResponse == null || documentResponse.getBody() == null) {
			throw UpstreamServiceException.invalidResponse("VIPS returned an empty document response for document " + documentId);
		}

		InputStream stream = DataBufferUtils.subscriberInputStream(
				documentResponse.getBody().onErrorMap(UpstreamServiceException::from), 1);
		byte[] prefix;
		long contentLength;
		boolean ready = false;
		try {
			prefix = stream.readNBytes(5);
			if (prefix.length == 0) {
				throw new ResourceNotFoundException("Document Not Found");
			}
			if (!new String(prefix, StandardCharsets.US_ASCII).equals("%PDF-")) {
				throw UpstreamServiceException.invalidResponse("VIPS returned non-PDF content for document " + documentId);
			}
			contentLength = documentResponse.getHeaders().getContentLength();
			ready = true;
		} catch (IOException e) {
			throw UpstreamServiceException.from(e);
		} catch (NumberFormatException e) {
			throw UpstreamServiceException.invalidResponse("VIPS returned an invalid document length.", e);
		} finally {
			if (!ready) {
				try {
					stream.close();
				} catch (IOException e) {
					logger.warn("Unable to close failed VIPS document stream", e);
				}
			}
		}

		InputStreamResource pdf = new InputStreamResource(new SequenceInputStream(
				new ByteArrayInputStream(prefix), stream)) {
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
