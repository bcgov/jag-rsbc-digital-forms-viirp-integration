package ca.bc.gov.open.digitalformsapi.viirp.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.ResourceNotFoundException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.UpstreamServiceException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.CodeTablesResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceRequest;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceRequest;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetDocumentsListServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.VipsNoticeObj;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import tools.jackson.databind.JsonNode;

/**
 * 
 * VIPS Rest Service operations Implementation
 * 
 * @author 237563
 *
 */
@Service
public class VipsRestServiceImpl implements VipsRestService {
	
	private final WebClient webClient;
	private final ConfigProperties properties;
	
	public VipsRestServiceImpl(WebClient webClient, ConfigProperties properties) {
		super();
		this.webClient = webClient;
		this.properties = properties;
	}

	@Override
	public CodeTablesResponse getCodeTableValues() {
		WebClient configurationClient = webClient.mutate()
				.codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
				.build();
		return readResponse(configurationClient
                .get()
                .uri("/configuration?includeImpoundLotOperatorDetails=true")
                .headers(headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()))
                .header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid())
                .header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
                .header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(VipsCodeTables::fromVips));
	}

	/**
	 * 
	 * createImpoundment. 
	 * Returns VIPS CreateImpoundmentServiceResponse type  
	 * 
	 */
	@Override
	public ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse createImpoundment(ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment impoundment) {
		
		return readResponse(webClient
                .post()
                .uri("/cases/impoundments")
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .bodyValue(new CreateImpoundmentServiceRequest(impoundment))
                .retrieve()
                .bodyToMono(ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse.class));
	}

	/**	  
	 * searchImpoundment. 
	 * 
	 * VIPS WS response object returned for given Notice No. 
 	 * 
	 */
	@Override
	public SearchImpoundmentsServiceResponse searchImpoundment(String noticeNo) {
		
		return readResponse(webClient
                .get()
                .uri("/cases/impoundments/search?impoundmentNoticeNo={noticeNo}", noticeNo)
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .retrieve()
                .bodyToMono(SearchImpoundmentsServiceResponse.class));
	}

	/**	  
	 * getImpoundment. 
	 * 
	 * VIPS WS response object returned given impoundment Id. 
 	 * 
	 */
	@Override
	public GetImpoundmentServiceResponse getImpoundment(Long impoundmentId) {
		
		return readResponse(webClient
                .get()
                .uri("/cases/impoundments/" + impoundmentId)
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .retrieve()
                .bodyToMono(GetImpoundmentServiceResponse.class));
	}
	
	
	/**	  
	 * searchProhibition. 
	 * 
	 * VIPS WS response object returned for given Notice No. 
 	 * 
	 */
	@Override
	public SearchProhibitionsServiceResponse searchProhibition(String noticeNo) {
		
		return readResponse(webClient
                .get()
                .uri("/cases/prohibitions/search?prohibitionNoticeNo={noticeNo}", noticeNo)
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .retrieve()
                .bodyToMono(SearchProhibitionsServiceResponse.class));
	}

	/**	  
	 * getProhibition. 
	 * 
	 * VIPS WS response object returned given prohibition Id. 
 	 * 
	 */
	@Override
	public GetProhibitionServiceResponse getProhibition(Long prohibitionId) {
		
		return readResponse(webClient
                .get()
                .uri("/cases/prohibitions/" + prohibitionId)
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .retrieve()
                .bodyToMono(GetProhibitionServiceResponse.class));
	}
	
	
	/**	  
	 * getDocument. 
	 * 
	 * VIPS WS response object returned for given document Id. 
 	 * 
	 */
	@Override
	public ResponseEntity<Flux<DataBuffer>> getDocument(Long documentId) {
		
		return readResponse(webClient
                .get()
                .uri("/documents/" + documentId + "?b64=false&url=false")
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .retrieve()
                .onStatus(status -> status.value() == 404, response -> response.releaseBody()
                        .then(Mono.error(new ResourceNotFoundException("Document Not Found"))))
                .toEntityFlux(DataBuffer.class));
	}

	/**
	 * 
	 * Associates Document ID To Notice No.
	 * 
	 * Returns a VIPS WS AssociateDocumentToNoticeServiceResponse response object.
	 * 
	 * @param documentId
	 * @param body
	 * @return
	 */
	@Override
	public AssociateDocumentToNoticeServiceResponse createDocumentAsociationPost(Long documentId, VipsNoticeObj body) {
		
		return readResponse(webClient
                .post()
                .uri("/document-association/notice/" + documentId)
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
        		.body(Mono.just(body), VipsNoticeObj.class) // body of request.
        		.retrieve()
                .bodyToMono(AssociateDocumentToNoticeServiceResponse.class));
	}

	/**
	 * 
	 * createProhibition. 
	 * Returns VIPS CreateProhibitionServiceResponse type  
	 * 
	 */
	@Override
	public CreateProhibitionServiceResponse createProhibition(CreateProhibition prohibition) {
		
		return readResponse(webClient
                .post()
                .uri("/cases/prohibitions")
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .bodyValue(new CreateProhibitionServiceRequest(prohibition))
                .retrieve()
                .bodyToMono(ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse.class));
	}

	/**
	 * 
	 * getDocumentsMetaList.
	 * VIPS WS response object returned given impoundment or prohibition Id. 
	 *
	 */
	@Override
	public GetDocumentsListServiceResponse getDocumentsMetaList(Long impoundmentId,
			Long prohibitionId) {
		
		String documentsUri = "/documents?";
		
		if (impoundmentId != null) {
			documentsUri = documentsUri + "impoundmentId=" + impoundmentId;
		} else {
			documentsUri = documentsUri + "prohibitionId=" + prohibitionId;
		}
		
		return readResponse(webClient
                .get()
                .uri(documentsUri)
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .retrieve()
                .bodyToMono(GetDocumentsListServiceResponse.class));
	}

	private <T> T readResponse(Mono<T> response) {
		return response
				.switchIfEmpty(Mono.error(UpstreamServiceException.invalidResponse("VIPS returned an empty response.")))
				.onErrorMap(UpstreamServiceException::from)
				.block();
	}

}
