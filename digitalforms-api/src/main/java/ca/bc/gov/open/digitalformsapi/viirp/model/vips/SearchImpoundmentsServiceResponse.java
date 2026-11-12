package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * SearchImpoundmentsServiceResponse - used to capture multiple responses from Impoundment Search.
 * 
 * 
 * @author smillar
 *
 */
public final class SearchImpoundmentsServiceResponse extends VipswsBasicResponse {

    private List<VipsImpoundmentBasicsObj> result = new ArrayList<VipsImpoundmentBasicsObj>();;
    private Long page; 
    private Long resultsPerPage; 
    private Long resultsCount;
    
    public SearchImpoundmentsServiceResponse(){};

    public SearchImpoundmentsServiceResponse(int respCd, String respMsg, Long page, Long resultsPerPage, long resultsCount, List<VipsImpoundmentBasicsObj> result){
        this.respCd = respCd;
        this.respMsg = respMsg;
        this.setResult(result);
    }

	public List<VipsImpoundmentBasicsObj> getResult() {
		return result;
	}

	public void setResult(List<VipsImpoundmentBasicsObj> result) {
		this.result = result;
	}

	/**
	 * @return the page
	 */
	public Long getPage() {
		return page;
	}

	/**
	 * @param page the page to set
	 */
	public void setPage(Long page) {
		this.page = page;
	}

	/**
	 * @return the resultsPerPage
	 */
	public Long getResultsPerPage() {
		return resultsPerPage;
	}

	/**
	 * @param resultsPerPage the resultsPerPage to set
	 */
	public void setResultsPerPage(Long resultsPerPage) {
		this.resultsPerPage = resultsPerPage;
	}

	/**
	 * @return the resultsCount
	 */
	public Long getResultsCount() {
		return resultsCount;
	}

	/**
	 * @param resultsCount the resultsCount to set
	 */
	public void setResultsCount(Long resultsCount) {
		this.resultsCount = resultsCount;
	}
	
	

}