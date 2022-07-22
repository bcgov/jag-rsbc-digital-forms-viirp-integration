package ca.bc.gov.open.digitalformsapi.viirp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;

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

}
