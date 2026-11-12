package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"registrationRoleTypeCd",
"organizationNm",
"surnameNm",
"firstGivenNm",
"secondGivenNm",
"thirdGivenNm",
"driverLicenceNo"
})
public class VipsImpoundmentRegistrationObj {
	
	@JsonProperty("registrationRoleTypeCd")
	private String registrationRoleTypeCd;
	
	@JsonProperty("organizationNm")
	private String organizationNm;
	
	@JsonProperty("surnameNm")
	private String surnameNm;
	
	@JsonProperty("firstGivenNm")
	private String firstGivenNm;
	
	@JsonProperty("secondGivenNm")
	private String secondGivenNm;
	
	@JsonProperty("thirdGivenNm")
	private String thirdGivenNm;
	
	@JsonProperty("driverLicenceNo")
	private String driverLicenceNo; 
	
	@JsonProperty("registrationRoleTypeCd")
	public String getRegistrationRoleTypeCd() {
	return registrationRoleTypeCd;
	}

	@JsonProperty("registrationRoleTypeCd")
	public void setRegistrationRoleTypeCd(String registrationRoleTypeCd) {
	this.registrationRoleTypeCd = registrationRoleTypeCd;
	}

	@JsonProperty("organizationNm")
	public String getOrganizationNm() {
	return organizationNm;
	}

	@JsonProperty("organizationNm")
	public void setOrganizationNm(String organizationNm) {
	this.organizationNm = organizationNm;
	}

	@JsonProperty("surnameNm")
	public String getSurnameNm() {
	return surnameNm;
	}

	@JsonProperty("surnameNm")
	public void setSurnameNm(String surnameNm) {
	this.surnameNm = surnameNm;
	}

	@JsonProperty("firstGivenNm")
	public String getFirstGivenNm() {
	return firstGivenNm;
	}

	@JsonProperty("firstGivenNm")
	public void setFirstGivenNm(String firstGivenNm) {
	this.firstGivenNm = firstGivenNm;
	}

	@JsonProperty("secondGivenNm")
	public String getSecondGivenNm() {
	return secondGivenNm;
	}

	@JsonProperty("secondGivenNm")
	public void setSecondGivenNm(String secondGivenNm) {
	this.secondGivenNm = secondGivenNm;
	}

	@JsonProperty("thirdGivenNm")
	public String getThirdGivenNm() {
	return thirdGivenNm;
	}

	@JsonProperty("thirdGivenNm")
	public void setThirdGivenNm(String thirdGivenNm) {
	this.thirdGivenNm = thirdGivenNm;
	}

	@JsonProperty("driverLicenceNo")
	public String getDriverLicenceNo() {
	return driverLicenceNo;
	}

	@JsonProperty("driverLicenceNo")
	public void setDriverLicenceNo(String driverLicenceNo) {
	this.driverLicenceNo = driverLicenceNo;
	}	

}
