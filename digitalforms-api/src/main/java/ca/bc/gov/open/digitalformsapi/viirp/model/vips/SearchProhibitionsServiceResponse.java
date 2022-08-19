package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * SearchProhibitionsServiceResponse - used to hold response from Prohibition Search.
 * 
 * 
 * @author smillar
 *
 */
public final class SearchProhibitionsServiceResponse extends VipswsBasicResponse {

    private List<ProhibitionSearchResponseType> result = new ArrayList<ProhibitionSearchResponseType>();
    private Long page; 
    private Long resultsPerPage; 
    private Long resultsCount;
    
    public SearchProhibitionsServiceResponse(){};

    public SearchProhibitionsServiceResponse(int respCd, String respMsg, Long page, Long resultsPerPage, long resultsCount, List<ProhibitionSearchResponseType> result){
        this.respCd = respCd;
        this.respMsg = respMsg;
        this.setResult(result);
        this.page = page;
        this.resultsPerPage = resultsPerPage; 
        this.resultsCount = resultsCount; 
    }

	public List<ProhibitionSearchResponseType> getResult() {
		return result;
	}

	public void setResult(List<ProhibitionSearchResponseType> result) {
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