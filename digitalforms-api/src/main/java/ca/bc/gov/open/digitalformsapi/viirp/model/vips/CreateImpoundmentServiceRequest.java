package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.ImpoundmentCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.RegistrationCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VehicleCreate;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

public record CreateImpoundmentServiceRequest(
        @JsonProperty("vipsImpoundCreate") ImpoundmentDetails impoundment,
        @JsonProperty("vipsRegistrationCreateArray") List<VipsRegistrationCreate> registrations,
        @JsonProperty("vipsVehicleCreate") VehicleCreate vehicle) {

    public CreateImpoundmentServiceRequest(CreateImpoundment request) {
        this(new ImpoundmentDetails(request.getImpoundment()),
                request.getRegistrations().stream().map(VipsRegistrationCreate::new).toList(),
                request.getVehicle());
    }

    @JsonProperty("vipsDocumentIdArray")
    public List<Long> documentIds() {
        return List.of();
    }

    @JsonProperty("vipsImpoundmentArray")
    public List<Long> priorImpoundmentIds() {
        return List.of();
    }

    public record ImpoundmentDetails(@JsonUnwrapped ImpoundmentCreate details) {

        @JsonProperty("noticeTypeCd")
        public String noticeTypeCd() {
            return "IMP";
        }

        @JsonProperty("noticeSubjectCd")
        public String noticeSubjectCd() {
            return "VEHI";
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
