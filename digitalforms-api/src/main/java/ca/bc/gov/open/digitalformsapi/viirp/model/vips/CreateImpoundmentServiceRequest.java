package ca.bc.gov.open.digitalformsapi.viirp.model.vips;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import ca.bc.gov.open.digitalformsapi.viirp.model.CreateImpoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.ImpoundmentCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.RegistrationCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.VehicleCreate;

public record CreateImpoundmentServiceRequest(
        @JsonProperty("vipsDocumentIdArray") List<Long> documentIds,
        @JsonProperty("vipsImpoundCreate") ImpoundmentDetails impoundment,
        @JsonProperty("vipsRegistrationCreateArray") List<RegistrationCreate> registrations,
        @JsonProperty("vipsVehicleCreate") VehicleCreate vehicle) {

    public CreateImpoundmentServiceRequest(CreateImpoundment request) {
        this(request.getDocumentIds(), new ImpoundmentDetails(request.getImpoundment()),
                request.getRegistrations(), request.getVehicle());
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
    }
}
