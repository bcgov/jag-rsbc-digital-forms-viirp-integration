package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

/**
 * 
 * GetProhibitionServiceResponse
 * 
 * 
 * @author smillar
 *
 */
public final class GetProhibitionServiceResponse extends VipswsBasicResponse {

    private ca.bc.gov.open.digitalformsapi.viirp.model.VipsProhibitionObj result;
    
    public GetProhibitionServiceResponse(){};

    public GetProhibitionServiceResponse(int respCd, String respMsg, ca.bc.gov.open.digitalformsapi.viirp.model.VipsProhibitionObj result){
        this.respCd = respCd;
        this.respMsg = respMsg;
        this.setResult(result);
    }

	public ca.bc.gov.open.digitalformsapi.viirp.model.VipsProhibitionObj getResult() {
		return result;
	}

	public void setResult(ca.bc.gov.open.digitalformsapi.viirp.model.VipsProhibitionObj result) {
		this.result = result;
	}

}