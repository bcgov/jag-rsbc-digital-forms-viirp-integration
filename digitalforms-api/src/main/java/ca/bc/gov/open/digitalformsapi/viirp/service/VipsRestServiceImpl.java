package ca.bc.gov.open.digitalformsapi.viirp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsGetDocumentByIdResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsNoticeObj;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import reactor.core.publisher.Mono;

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
	public GetCodetablesServiceResponse getCodeTableValues(String correlationId) {
		return webClient
                .get()
                .uri("/configuration")
                .headers(headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()))
                .retrieve()
                .bodyToMono(GetCodetablesServiceResponse.class)
                .doOnSuccess(System.out::println)
                .block();
	}

	/**
	 * 
	 * createImpoundment. 
	 * Returns VIPS CreateImpoundmentServiceResponse type  
	 * 
	 */
	@Override
	public ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse createImpoundment(String correlationId, ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment impoundment) {
		
		return webClient
                .post()
                .uri("/cases/impoundments")
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .body(Mono.just(impoundment), ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment.class) // body of request.
                .retrieve()
                .bodyToMono(ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse.class) // body of response (need VIPS class)
        		.block();
	}

	/**	  
	 * searchImpoundment. 
	 * 
	 * VIPS WS response object returned for given Notice No. 
 	 * 
	 */
	@Override
	public SearchImpoundmentsServiceResponse searchImpoundment(String correlationId, String noticeNo) {
		
		return webClient
                .get()
                .uri("/cases/impoundments/search?impoundmentNoticeNo=" + noticeNo)
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .retrieve()
                .bodyToMono(SearchImpoundmentsServiceResponse.class) // body of response (VIPS WS class)
        		.block();
	}

	/**	  
	 * getImpoundment. 
	 * 
	 * VIPS WS response object returned given impoundment Id. 
 	 * 
	 */
	@Override
	public GetImpoundmentServiceResponse getImpoundment(String correlationId, Long impoundmentId) {
		
		return webClient
                .get()
                .uri("/cases/impoundments/" + impoundmentId)
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .retrieve()
                .bodyToMono(GetImpoundmentServiceResponse.class) // body of response (VIPS WS class)
        		.block();
	}
	
	/**	  
	 * getDocument. 
	 * 
	 * VIPS WS response object returned for given document Id. 
 	 * 
	 */
	@Override
	public VipsGetDocumentByIdResponse getDocumentAsBase64(String correlationId, Long documentId) {
		
		return webClient
                .get()
                .uri("/documents/" + documentId + "?b64=true&url=false")
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .retrieve()
                .bodyToMono(String.class) // Get the base64 encoded string from the response
                .map(base64String -> {
                	// Add base64 string to the response object
                    VipsGetDocumentByIdResponse response = new VipsGetDocumentByIdResponse();
                    response.setDocument(base64String);
                    return response;
                })
        		.block();
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
		
		return webClient
                .post()
                .uri("/document-association/notice/" + documentId)
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
        		.body(Mono.just(body), VipsNoticeObj.class) // body of request.
        		.retrieve()
                .bodyToMono(AssociateDocumentToNoticeServiceResponse.class) // body of response (VIPS WS class)
        		.block();
	}

	/**
	 * 
	 * createProhibition. 
	 * Returns VIPS CreateProhibitionServiceResponse type  
	 * 
	 */
	@Override
	public CreateProhibitionServiceResponse createProhibition(String correlationId, CreateProhibition prohibition) {
		
		return webClient
                .post()
                .uri("/cases/prohibitions")
                .headers (headers -> headers.setBasicAuth(properties.getVipsRestApiUsername(), properties.getVipsRestApiPassword()) )
        		.header(DigitalFormsConstants.VIPS_API_HEADER_GUID, properties.getVipsRestApiCredentialsGuid()) 
        		.header(DigitalFormsConstants.VIPS_API_HEADER_DISPLAYNAME, properties.getVipsRestApiCredentialsDisplayname())
        		.header(DigitalFormsConstants.VIPS_API_HEADER_USER, properties.getVipsRestApiCredentialsUser())
                .body(Mono.just(prohibition), ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment.class) // body of request.
                .retrieve()
                .bodyToMono(ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse.class) // body of response (need VIPS class)
        		.block();
	}
	
}
