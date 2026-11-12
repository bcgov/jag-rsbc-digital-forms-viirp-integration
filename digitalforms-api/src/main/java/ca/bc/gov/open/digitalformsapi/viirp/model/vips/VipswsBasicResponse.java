package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

/**
 * 
 * VipswsBasicResponse
 * 
 * Base class for VIPSWS responses.
 * 
 * @author 176899
 *
 */
public class VipswsBasicResponse {
	
    protected int respCd = DigitalFormsConstants.VIPSWS_JAVA_EX; // not set default - possible JAVA fault., 0 == good, 1 == bad
    protected String respMsg;
    
	public int getRespCd() {
		return respCd;
	}
	public void setRespCd(int respCd) {
		this.respCd = respCd;
	}
	public String getRespMsg() {
		return respMsg;
	}
	public void setRespMsg(String respMsg) {
		this.respMsg = respMsg;
	}
  
}
