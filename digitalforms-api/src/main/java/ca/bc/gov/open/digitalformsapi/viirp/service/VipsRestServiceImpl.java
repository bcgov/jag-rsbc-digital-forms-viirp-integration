package ca.bc.gov.open.digitalformsapi.viirp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import lombok.AllArgsConstructor;

/**
 * 
 * VIPS Rest Service operations Implementation
 * 
 * @author 237563
 *
 */
@Service
@AllArgsConstructor
public class VipsRestServiceImpl implements VipsRestService {
	
	private final WebClient webClient;
	private final DigitalFormsConstants constants;

	@Override
	public GetCodetablesServiceResponse getCodeTableValues(String correlationId) {
		return webClient
                .get()
                .uri("/configuration")
                .headers(headers -> headers.setBasicAuth(constants.getVipsRestApiUsername(), constants.getVipsRestApiPassword()))
                .retrieve()
                .bodyToMono(GetCodetablesServiceResponse.class)
                .doOnSuccess(System.out::println)
                .block();
	}

}
