package ca.bc.gov.open.digitalformsapi.viirp.service;

import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;

/**
 * 
 * VIPS Rest Service operations Interface
 * 
 * @author 237563
 *
 */
public interface VipsRestService {
	
	/**
	 * Retrieves a {@link GetCodetablesServiceResponse} by proxying the VIPS WS call to the 'configuration' endpoint
	 * 
	 * @param correlationId
	 * @return
	 */
	public GetCodetablesServiceResponse getCodeTableValues(String correlationId);
	
	/**
	 * 
	 * Retrieves a {@link CreateImpoundmentServiceResponse} by proxying the VIPS WS call to the POST 'impoundment' endpoint
	 * 
	 * @param correlationId
	 * @param impoundment
	 * @return
	 */
	public ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateImpoundmentServiceResponse createImpoundment(String correlationId, ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment impoundment);
	
}
