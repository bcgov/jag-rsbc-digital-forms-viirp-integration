package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

/**
 * 
 * CreateProhibitionServiceResponse
 * 
 * 
 * @author smillar
 *
 */
public final class CreateProhibitionServiceResponse extends VipswsBasicResponse {
	
	private Long prohibitionId; 
    
    public CreateProhibitionServiceResponse(){};

    public CreateProhibitionServiceResponse (int respCd, String respMsg, Long result){
        this.respCd = respCd;
        this.respMsg = respMsg;
        this.setProhibitionId(result);
    }

	public Long getProhibitionId() {
		return prohibitionId;
	}

	public void setProhibitionId(Long prohibitionId) {
		this.prohibitionId = prohibitionId;
	}

}