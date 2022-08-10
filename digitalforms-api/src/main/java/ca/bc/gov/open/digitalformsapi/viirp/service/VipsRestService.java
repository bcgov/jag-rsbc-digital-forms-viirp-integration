package ca.bc.gov.open.digitalformsapi.viirp.service;

import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsGetDocumentByIdResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse;

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

	/**
	 * 
	 * Returns a {@link SearchImpoundmentsServiceResponse} by calling VIPS WS to search for a given Notice Number.
	 * 
	 * @See ImpoundmentsApiDelegateImpl. 
	 * 
	 * @param correlationId
	 * @param noticeNo
	 * @return
	 */
	public SearchImpoundmentsServiceResponse searchImpoundment(String correlationId, String noticeNo);
	
	
	/**
	 * 
	 * Returns a {@link SearchImpoundmentsServiceResponse} by calling VIPS WS to search for a given impoundment Id.
	 * 
	 * 
	 * @param correlationId
	 * @param impoundmentId
	 * @return
	 */
	public GetImpoundmentServiceResponse getImpoundment(String correlationId, Long impoundmentId);
	
	/**
	 * 
	 * Returns a {@link VipsGetDocumentByIdResponse} by calling VIPS WS to return a document Base64 for a given document Id.
	 * 
	 * @param correlationId
	 * @param documentId
	 * @return
	 */
	public VipsGetDocumentByIdResponse getDocumentAsBase64(String correlationId, Long documentId);
	
}
