package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import java.util.List;

import ca.bc.gov.open.digitalformsapi.viirp.model.Document;

/**
 * 
 * GetDocumentsListServiceResponse - used to hold response from VIPS Get Documents Meta List
 * 
 * @author 237563
 *
 */
public final class GetDocumentsListServiceResponse extends VipswsBasicResponse {

	private List<Document> results = null;
    
    public GetDocumentsListServiceResponse(){};

    public GetDocumentsListServiceResponse(int respCd, String respMsg, List<Document> results){
        this.respCd = respCd;
        this.respMsg = respMsg;
        this.setResult(results);
    }

	public List<Document> getResults() {
		return results;
	}

	public void setResult(List<Document> results) {
		this.results = results;
	}

}