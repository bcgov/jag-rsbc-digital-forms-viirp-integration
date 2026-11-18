package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import ca.bc.gov.open.digitalformsapi.viirp.model.Prohibition;

/**
 * 
 * GetProhibitionServiceResponse
 * 
 * 
 * @author smillar
 *
 */
public final class GetProhibitionServiceResponse extends VipswsBasicResponse {

    private Prohibition result;
    
    public GetProhibitionServiceResponse(){};

    public GetProhibitionServiceResponse(int respCd, String respMsg, Prohibition result){
        this.respCd = respCd;
        this.respMsg = respMsg;
        this.setResult(result);
    }

	public Prohibition getResult() {
		return result;
	}

	public void setResult(Prohibition result) {
		this.result = result;
	}

}