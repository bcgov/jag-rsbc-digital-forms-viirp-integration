package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import ca.bc.gov.open.digitalformsapi.viirp.model.CreateProhibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.ProhibitionCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.RegistrationCreate;

public record CreateProhibitionServiceRequest(
        @JsonProperty("vipsProhibitionCreate") ProhibitionDetails prohibition,
        @JsonProperty("vipsRegistrationCreate") RegistrationCreate registration) {

    public CreateProhibitionServiceRequest(CreateProhibition request) {
        this(new ProhibitionDetails(request.getProhibition()), request.getRegistration());
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
    }
}
