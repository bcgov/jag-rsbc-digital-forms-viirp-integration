package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

/**
 * 
 * CreateImpoundmentServiceResponse
 * 
 * 
 * @author smillar
 *
 */
public final class CreateImpoundmentServiceResponse extends VipswsBasicResponse {
	
	private Long impoundmentId;
    
    public CreateImpoundmentServiceResponse(){};

    public CreateImpoundmentServiceResponse(int respCd, String respMsg, Long impoundmentId){
        this.respCd = respCd;
        this.respMsg = respMsg;
        this.impoundmentId = impoundmentId; 
    }
    
    public CreateImpoundmentServiceResponse(int respCd, String respMsg){
        this.respCd = respCd;
        this.respMsg = respMsg;
    }

	public Long getImpoundmentId() {
		return impoundmentId;
	}

	public void setImpoundmentId(Long impoundmentId) {
		this.impoundmentId = impoundmentId;
	}

}