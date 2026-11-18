package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import ca.bc.gov.open.digitalformsapi.viirp.model.Impoundment;

/**
 * 
 * GetImpoundmentServiceResponse
 * 
 * 
 * @author smillar
 *
 */
public final class GetImpoundmentServiceResponse extends VipswsBasicResponse {

    private Impoundment result;
    
    public GetImpoundmentServiceResponse(){};

    public GetImpoundmentServiceResponse(int respCd, String respMsg, Impoundment result){
        this.respCd = respCd;
        this.respMsg = respMsg;
        this.setResult(result);
    }

	public Impoundment getResult() {
		return result;
	}

	public void setResult(Impoundment result) {
		this.result = result;
	}

}