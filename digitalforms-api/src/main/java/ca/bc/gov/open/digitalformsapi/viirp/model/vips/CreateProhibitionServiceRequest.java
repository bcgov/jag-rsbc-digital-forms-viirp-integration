package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.ProhibitionCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.RegistrationCreate;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

public record CreateProhibitionServiceRequest(
        @JsonProperty("vipsProhibitionCreate") ProhibitionDetails prohibition,
        @JsonProperty("vipsRegistrationCreate") VipsRegistrationCreate registration) {

    public CreateProhibitionServiceRequest(CreateProhibition request) {
        this(new ProhibitionDetails(request.getProhibition()), new VipsRegistrationCreate(request.getRegistration()));
    }

    @JsonProperty("vipsDocumentIdArray")
    public List<Long> documentIds() {
        return List.of();
    }

    public record ProhibitionDetails(@JsonUnwrapped ProhibitionCreate details) {

        @JsonProperty("noticeSubjectCd")
        public String noticeSubjectCd() {
            return "PERS";
        }

        @JsonProperty("dataSourceCd")
        public String dataSourceCd() {
            return DigitalFormsConstants.DIGITAL_FORMS_DATA_SOURCE_CD;
        }
    }

    public record VipsRegistrationCreate(@JsonUnwrapped RegistrationCreate details) {

        @JsonProperty("dataSourceCd")
        public String dataSourceCd() {
            return DigitalFormsConstants.DIGITAL_FORMS_DATA_SOURCE_CD;
        }
    }
}
