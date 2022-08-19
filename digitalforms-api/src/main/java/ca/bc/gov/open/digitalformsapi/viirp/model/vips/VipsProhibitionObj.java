package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import ca.bc.gov.open.digitalformsapi.viirp.model.VipsDocumentObj;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsRegistrationObj;
import ca.bc.gov.open.digitalformsapi.viirp.utils.vips.TimeDateUtils;


/**
 * 
 * VipsProhibtionObj - mimics 'vips_prohibtion_obj'
 * 
 * @author smillar
 *
 */
public class VipsProhibitionObj {

	@JsonProperty("prohibitionId") // 15
	private Long prohibtionId = null;

	@JsonProperty("prohibitionNoticeNo") // 12
	private String prohibitionNoticeNo = null;

	@JsonProperty("policeDetatchmentId") // 15 -R
	private Long policeDetatchmentId = null;

	@JsonProperty("driverLicenceSeizedYN") // 1 
	private String driverLicenceSeizedYN = null;

	@JsonProperty("licenceId") // 15
	private Long licenceId = null;

	@JsonProperty("noticeSubjectCd") // 10 -R
	private String noticeSubjectCd = null;

	@JsonProperty("noticeTypeCd") // 10 -R
	private String noticeTypeCd = null;

	@JsonProperty("noticeServedDt") // -R
	private Date noticeServedDt = null;

	@JsonProperty("streetDetailsTxt") // 4000
	private String streetDetailsTxt = null;

	@JsonProperty("incidentDtm")
	private Date incidentDtm = null;

	@JsonProperty("policeFileNo") // 15
	private String policeFileNo = null;

	@JsonProperty("effectiveDt")
	private Date effectiveDt = null;

	@JsonProperty("expiryDt")
	private Date expiryDt = null;

	@JsonProperty("policeOfficerNm") // 60
	private String policeOfficerNm = null;

	@JsonProperty("policeOfficerNo") // 30
	private String policeOfficerNo = null;

	@JsonProperty("tempDlEffectiveDt")
	private Date tempDlEffectiveDt = null;

	@JsonProperty("tempDlExpiryDt")
	private Date tempDlExpiryDt = null;

	@JsonProperty("cityNm") // 30
	private String cityNm = null;

	@JsonProperty("provinceCd") // 2
	private String provinceCd = null;

	@JsonProperty("originalCauseCd") // 10 -R
	private String originalCauseCd = null;
	
	@JsonProperty("icbcTransmissionCd") 
	private String icbcTransmissionCd = null;
	
	@JsonProperty("dreEvaluationCds") 
	private List<String> dreEvaluationCds = new ArrayList<String>();
	
	@JsonProperty("cancelled") 
	private boolean cancelled;

	@JsonProperty("registration")
	private VipsRegistrationObj driver = new VipsRegistrationObj();

	// Flagged for removal
	//@JsonProperty("licence")
	//private VipsLicenceObj licence = new VipsLicenceObj();

	@JsonProperty("documents")
	private List<VipsDocumentObj> documents = new ArrayList<VipsDocumentObj>();

	public Long getProhibtionId() {
		return prohibtionId;
	}

	public void setProhibtionId(Long prohibtionId) {
		this.prohibtionId = prohibtionId;
	}

	public String getProhibitionNoticeNo() {
		return prohibitionNoticeNo;
	}

	public void setProhibitionNoticeNo(String prohibitionNoticeNo) {
		this.prohibitionNoticeNo = prohibitionNoticeNo;
	}

	public Long getPoliceDetatchmentId() {
		return policeDetatchmentId;
	}

	public void setPoliceDetatchmentId(Long policeDetatchmentId) {
		this.policeDetatchmentId = policeDetatchmentId;
	}

	public String getDriverLicenceSeizedYN() {
		return driverLicenceSeizedYN;
	}

	public void setDriverLicenceSeizedYN(String driverLicenceSeizedYN) {
		this.driverLicenceSeizedYN = driverLicenceSeizedYN;
	}

	public Long getLicenceId() {
		return licenceId;
	}

	public void setLicenceId(Long licenceId) {
		this.licenceId = licenceId;
	}

	public String getNoticeSubjectCd() {
		return noticeSubjectCd;
	}

	public void setNoticeSubjectCd(String noticeSubjectCd) {
		this.noticeSubjectCd = noticeSubjectCd;
	}

	public String getNoticeTypeCd() {
		return noticeTypeCd;
	}

	public void setNoticeTypeCd(String noticeTypeCd) {
		this.noticeTypeCd = noticeTypeCd;
	}

	public String getNoticeServedDt() {
		if ( null != noticeServedDt) {
			return TimeDateUtils.getISO8601FromDate(noticeServedDt);
		} else {
			return null;
		}
	}

	public void setNoticeServedDt(Date noticeServedDt) {
		this.noticeServedDt = noticeServedDt;
	}

	public String getStreetDetailsTxt() {
		return streetDetailsTxt;
	}

	public void setStreetDetailsTxt(String streetDetailsTxt) {
		this.streetDetailsTxt = streetDetailsTxt;
	}

	public String getIncidentDtm() {
		if ( null != incidentDtm) {
			return TimeDateUtils.getISO8601FromDate(incidentDtm);
		} else {
			return null;
		}
	}

	public void setIncidentDtm(Date incidentDtm) {
		this.incidentDtm = incidentDtm;
	}

	public String getPoliceFileNo() {
		return policeFileNo;
	}

	public void setPoliceFileNo(String policeFileNo) {
		this.policeFileNo = policeFileNo;
	}

	public String getEffectiveDt() {
		if ( null != effectiveDt) {
			return TimeDateUtils.getISO8601FromDate(effectiveDt);
		} else {
			return null;
		}
	}

	public void setEffectiveDt(Date effectiveDt) {
		this.effectiveDt = effectiveDt;
	}

	public String getExpiryDt() {
		if ( null !=  expiryDt) {
			return TimeDateUtils.getISO8601FromDate(expiryDt);
		} else {
			return null;
		}
	}

	public void setExpiryDt(Date expiryDt) {
		this.expiryDt = expiryDt;
	}

	public String getPoliceOfficerNm() {
		return policeOfficerNm;
	}

	public void setPoliceOfficerNm(String policeOfficerNm) {
		this.policeOfficerNm = policeOfficerNm;
	}

	public String getPoliceOfficerNo() {
		return policeOfficerNo;
	}

	public void setPoliceOfficerNo(String policeOfficerNo) {
		this.policeOfficerNo = policeOfficerNo;
	}

	public String getTempDlEffectiveDt() {
		if ( null !=  tempDlEffectiveDt) {
			return TimeDateUtils.getISO8601FromDate(tempDlEffectiveDt);
		} else {
			return null;
		}
	}

	public void setTempDlEffectiveDt(Date tempDlEffectiveDt) {
		this.tempDlEffectiveDt = tempDlEffectiveDt;
	}

	public String getTempDlExpiryDt() {
		if ( null !=  tempDlExpiryDt) {
			return TimeDateUtils.getISO8601FromDate(tempDlExpiryDt);
		} else {
			return null;
		}
	}

	public void setTempDlExpiryDt(Date tempDlExpiryDt) {
		this.tempDlExpiryDt = tempDlExpiryDt;
	}

	public String getCityNm() {
		return cityNm;
	}

	public void setCityNm(String cityNm) {
		this.cityNm = cityNm;
	}

	public String getProvinceCd() {
		return provinceCd;
	}

	public void setProvinceCd(String provinceCd) {
		this.provinceCd = provinceCd;
	}

	public String getOriginalCauseCd() {
		return originalCauseCd;
	}

	public void setOriginalCauseCd(String originalCauseCd) {
		this.originalCauseCd = originalCauseCd;
	}

	public String getIcbcTransmissionCd() {
		return icbcTransmissionCd;
	}

	public void setIcbcTransmissionCd(String icbcTransmissionCd) {
		this.icbcTransmissionCd = icbcTransmissionCd;
	}

	public List<String> getDreEvaluationCds() {
		return dreEvaluationCds;
	}

	public void setDreEvaluationCds(List<String> dreEvaluationCds) {
		this.dreEvaluationCds = dreEvaluationCds;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public VipsRegistrationObj getDriver() {
		return driver;
	}

	public void setDriver(VipsRegistrationObj driver) {
		this.driver = driver;
	}

//  Flagged for removal	
//	public VipsLicenceObj getLicence() {
//		return licence;
//	}
//
//	public void setLicence(VipsLicenceObj licence) {
//		this.licence = licence;
//	}

	public List<VipsDocumentObj> getDocuments() {
		return documents;
	}

	public void setDocuments(List<VipsDocumentObj> documents) {
		this.documents = documents;
	}

}
