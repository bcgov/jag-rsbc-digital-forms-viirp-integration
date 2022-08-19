package ca.bc.gov.open.digitalformsapi.viirp.service;

import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsGetDocumentByIdResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsNoticeObj;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.AssociateDocumentToNoticeServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetImpoundmentServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.GetProhibitionServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchImpoundmentsServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.vips.SearchProhibitionsServiceResponse;

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
	 * Returns a {@link SearchProhibitionsServiceResponse} by calling VIPS WS to search for a given Notice Number.
	 * 
	 * @See ProhibitionsApiDelegateImpl. 
	 * 
	 * @param correlationId
	 * @param noticeNo
	 * @return
	 */
	public SearchProhibitionsServiceResponse searchProhibition(String correlationId, String noticeNo);
	
	
	/**
	 * 
	 * Returns a {@link SearchProhibitionServiceResponse} by calling VIPS WS to search for a given impoundment Id.
	 * 
	 * 
	 * @param correlationId
	 * @param ProhibitionId
	 * @return
	 */
	public GetProhibitionServiceResponse getProhibition(String correlationId, Long prohibitionId);
	
	
	/**
	 * 
	 * Returns a {@link VipsGetDocumentByIdResponse} by calling VIPS WS to return a document Base64 for a given document Id.
	 * 
	 * @param correlationId
	 * @param documentId
	 * @return
	 */
	public VipsGetDocumentByIdResponse getDocumentAsBase64(String correlationId, Long documentId);

	
	/**
	 * 
	 * Returns a {@link AssociateDocumentToNoticeServiceResponse} by posting Notice and Type to VIPS WS.
	 * 
	 * @param documentId
	 * @param body
	 * @return
	 */
	public AssociateDocumentToNoticeServiceResponse createDocumentAsociationPost(Long documentId, VipsNoticeObj body);
	
	/**
	 * 
	 * Returns a {@link CreateProhibitionServiceResponse} by proxying the VIPS WS call to the POST 'prohibition' endpoint
	 * 
	 * @param correlationId
	 * @param prohibition
	 * @return
	 */
	public ca.bc.gov.open.digitalformsapi.viirp.model.vips.CreateProhibitionServiceResponse createProhibition(String correlationId, ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition impoundment);
	
}
