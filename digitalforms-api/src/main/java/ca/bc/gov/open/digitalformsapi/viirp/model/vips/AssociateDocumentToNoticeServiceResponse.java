package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

/**
 * 
 * AssociateDocumentToNoticeServiceResponse
 * 
 * 
 * @author smillar
 *
 */
public final class AssociateDocumentToNoticeServiceResponse extends VipswsBasicResponse {
    
    public AssociateDocumentToNoticeServiceResponse(){};

    public AssociateDocumentToNoticeServiceResponse(int respCd, String respMsg){
        this.respCd = respCd;
        this.respMsg = respMsg;
    }

}