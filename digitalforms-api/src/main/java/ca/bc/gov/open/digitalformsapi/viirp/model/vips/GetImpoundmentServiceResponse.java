package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import ca.bc.gov.open.digitalformsapi.viirp.model.VipsImpoundObj;

/**
 * 
 * GetImpoundmentServiceResponse
 * 
 * 
 * @author smillar
 *
 */
public final class GetImpoundmentServiceResponse extends VipswsBasicResponse {

    private VipsImpoundObj result;
    
    public GetImpoundmentServiceResponse(){};

    public GetImpoundmentServiceResponse(int respCd, String respMsg, VipsImpoundObj result){
        this.respCd = respCd;
        this.respMsg = respMsg;
        this.setResult(result);
    }

	public VipsImpoundObj getResult() {
		return result;
	}

	public void setResult(VipsImpoundObj result) {
		this.result = result;
	}

}