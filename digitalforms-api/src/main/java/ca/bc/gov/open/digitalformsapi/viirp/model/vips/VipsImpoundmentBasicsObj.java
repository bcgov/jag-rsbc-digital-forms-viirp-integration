package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import ca.bc.gov.open.digitalformsapi.viirp.utils.vips.TimeDateUtils;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "impoundmentId", "impoundmentNoticeNo", "policeFileNo", "impoundmentDt", "driverLicenceNo",
		"policeOfficerNo", "projectedReleaseDt", "originalCauseCds", "registrations" })
public class VipsImpoundmentBasicsObj {

	@JsonProperty("impoundmentId")
	private Long impoundmentId;

	@JsonProperty("impoundmentNoticeNo")
	private String impoundmentNoticeNo;

	@JsonProperty("policeFileNo")
	private String policeFileNo;

	@JsonProperty("impoundmentDt")
	private Date impoundmentDt;

	@JsonProperty("driverLicenceNo")
	private String driverLicenceNo;

	@JsonProperty("policeOfficerNo")
	private String policeOfficerNo;

	@JsonProperty("projectedReleaseDt")
	private Date projectedReleaseDt;

	@JsonProperty("originalCauseCds")
	private List<String> originalCauseCds = null;

	@JsonProperty("registrations")
	private List<VipsImpoundmentRegistrationObj> registrations = null;

	@JsonProperty("impoundmentId")
	public Long getImpoundmentId() {
		return impoundmentId;
	}

	@JsonProperty("impoundmentId")
	public void setImpoundmentId(Long impoundmentId) {
		this.impoundmentId = impoundmentId;
	}

	@JsonProperty("impoundmentNoticeNo")
	public String getImpoundmentNoticeNo() {
		return impoundmentNoticeNo;
	}

	@JsonProperty("impoundmentNoticeNo")
	public void setImpoundmentNoticeNo(String impoundmentNoticeNo) {
		this.impoundmentNoticeNo = impoundmentNoticeNo;
	}

	@JsonProperty("policeFileNo")
	public String getPoliceFileNo() {
		return policeFileNo;
	}

	@JsonProperty("policeFileNo")
	public void setPoliceFileNo(String policeFileNo) {
		this.policeFileNo = policeFileNo;
	}

	@JsonProperty("impoundmentDt")
	public String getImpoundmentDt() {
		if (null != this.impoundmentDt) {
			return TimeDateUtils.getISO8601FromDate(this.impoundmentDt); 
		} else {
			return null; 
		}
	}

	@JsonProperty("impoundmentDt")
	public void setImpoundmentDt(Date impoundmentDt) {
		this.impoundmentDt = impoundmentDt;
	}

	@JsonProperty("driverLicenceNo")
	public String getDriverLicenceNo() {
		return driverLicenceNo;
	}

	@JsonProperty("driverLicenceNo")
	public void setDriverLicenceNo(String driverLicenceNo) {
		this.driverLicenceNo = driverLicenceNo;
	}

	@JsonProperty("policeOfficerNo")
	public String getPoliceOfficerNo() {
		return policeOfficerNo;
	}

	@JsonProperty("policeOfficerNo")
	public void setPoliceOfficerNo(String policeOfficerNo) {
		this.policeOfficerNo = policeOfficerNo;
	}

	@JsonProperty("projectedReleaseDt")
	public String getProjectedReleaseDt() {
		if (null != this.projectedReleaseDt) {
			return TimeDateUtils.getISO8601FromDate(this.projectedReleaseDt); 
		} else {
			return null; 
		}
	}

	@JsonProperty("projectedReleaseDt")
	public void setProjectedReleaseDt(Date projectedReleaseDt) {
		this.projectedReleaseDt = projectedReleaseDt;
	}

	@JsonProperty("originalCauseCds")
	public List<String> getOriginalCauseCds() {
		if (null == this.originalCauseCds) {
			this.originalCauseCds = new ArrayList<String>();
			return this.originalCauseCds;
		} else {
			return this.originalCauseCds;
		}
	}

	@JsonProperty("originalCauseCds")
	public void setOriginalCauseCds(List<String> originalCauseCds) {
		this.originalCauseCds = originalCauseCds;
	}

	@JsonProperty("registrations")
	public List<VipsImpoundmentRegistrationObj> getRegistrations() {
		if (null == this.registrations) {
			this.registrations = new ArrayList<VipsImpoundmentRegistrationObj>();
			return this.registrations;
		}
		return registrations;
	}

	@JsonProperty("registrations")
	public void setRegistrations(List<VipsImpoundmentRegistrationObj> registrations) {
		this.registrations = registrations;
	}

}
