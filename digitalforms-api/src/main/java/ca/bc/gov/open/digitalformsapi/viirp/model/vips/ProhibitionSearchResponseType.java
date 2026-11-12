package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import ca.bc.gov.open.digitalformsapi.viirp.utils.vips.TimeDateUtils;


/**
 * 
 * ProhibitionSearchResponseType - Captures each record returned from the Prohib Search
 * 
 * @see ca.bc.gov.pssg.rsbc.vipsws.model.SearchProhibitionServiceResponse
 * @see ca.bc.gov.pssg.rsbc.vipsws.procedures.SearchProhibitionStoredProcedure
 * 
 * @author smillar
 *
 */
public class ProhibitionSearchResponseType {

	@JsonProperty("prohibitionId")
	private Long prohibitionId = null;

	@JsonProperty("prohibitionNoticeNo")
	private String prohibitionNoticeNo = null;

	@JsonProperty("noticeServedDt")
	private Date noticeServedDt = null;

	@JsonProperty("surnameNm")
	private String surnameNm = null;

	@JsonProperty("firstGivenNm")
	private String firstGivenNm = null;

	@JsonProperty("secondGivenNm")
	private String secondGivenNm = null;

	@JsonProperty("thirdGivenNm")
	private String thirdGivenNm = null;

	@JsonProperty("driverLicenceNo")
	private String driverLicenceNo = null;

	@JsonProperty("originalCauseCd")
	private String originalCauseCd = null;

	@JsonProperty("originalCauseDsc")
	private String originalCauseDsc = null;
	
	@JsonProperty("policeFileNo")
	private String policeFileNo = null;

	public Long getProhibitionId() {
		return prohibitionId;
	}

	public void setProhibitionId(Long prohibitionId) {
		this.prohibitionId = prohibitionId;
	}

	public String getProhibitionNoticeNo() {
		return prohibitionNoticeNo;
	}

	public void setProhibitionNoticeNo(String prohibitionNoticeNo) {
		this.prohibitionNoticeNo = prohibitionNoticeNo;
	}

	public String getNoticeServedDt() {
		if ( null != this.noticeServedDt ) {
			return TimeDateUtils.getISO8601FromDate(noticeServedDt); 
		} else {
			return null; 
		}
	}

	public void setNoticeServedD(Date noticeServedDt) {
		this.noticeServedDt = noticeServedDt;
	}

	public String getSurnameNm() {
		return surnameNm;
	}

	public void setSurnameNm(String surnameNm) {
		this.surnameNm = surnameNm;
	}

	public String getFirstGivenNm() {
		return firstGivenNm;
	}

	public void setFirstGivenNm(String firstGivenNm) {
		this.firstGivenNm = firstGivenNm;
	}

	public String getSecondGivenNm() {
		return secondGivenNm;
	}

	public void setSecondGivenNm(String secondGivenNm) {
		this.secondGivenNm = secondGivenNm;
	}

	public String getThirdGivenNm() {
		return thirdGivenNm;
	}

	public void setThirdGivenNm(String thirdGivenNm) {
		this.thirdGivenNm = thirdGivenNm;
	}

	public String getDriverLicenceNo() {
		return driverLicenceNo;
	}

	public void setDriverLicenceNo(String driverLicenceNo) {
		this.driverLicenceNo = driverLicenceNo;
	}

	public String getOriginalCauseCd() {
		return originalCauseCd;
	}

	public void setOriginalCauseCd(String originalCauseCd) {
		this.originalCauseCd = originalCauseCd;
	}

	public String getOriginalCauseDsc() {
		return originalCauseDsc;
	}

	public void setOriginalCauseDsc(String originalCauseDsc) {
		this.originalCauseDsc = originalCauseDsc;
	}

	public String getPoliceFileNo() {
		return policeFileNo;
	}
	
	public void setPoliceFileNo(String policeFileNo) {
		this.policeFileNo = policeFileNo;
	}
	
}
