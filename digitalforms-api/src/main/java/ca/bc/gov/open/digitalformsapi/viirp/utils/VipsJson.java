package ca.bc.gov.open.digitalformsapi.viirp.utils;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import ca.bc.gov.open.digitalformsapi.viirp.model.Address;
import ca.bc.gov.open.digitalformsapi.viirp.model.AddressCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.CodeDescription;
import ca.bc.gov.open.digitalformsapi.viirp.model.CodeTables;
import ca.bc.gov.open.digitalformsapi.viirp.model.Document;
import ca.bc.gov.open.digitalformsapi.viirp.model.Impoundment;
import ca.bc.gov.open.digitalformsapi.viirp.model.ImpoundmentCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.Licence;
import ca.bc.gov.open.digitalformsapi.viirp.model.LicenceCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.Prohibition;
import ca.bc.gov.open.digitalformsapi.viirp.model.ProhibitionCreate;
import ca.bc.gov.open.digitalformsapi.viirp.model.Registration;
import ca.bc.gov.open.digitalformsapi.viirp.model.RegistrationCreate;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdScalarSerializer;

/**
 * The VIPS WS wire names and {@code "Y"}/{@code "N"} flags for the public API models, used only by
 * the VIPS WS client. The public API exposes consistent names and JSON booleans; VIPS WS keeps its
 * historical names (e.g. {@code policeDetatchmentId}, {@code driverLicenceSeizedYN}).
 */
public final class VipsJson {

    private VipsJson() {
    }

    public static JacksonModule module() {
        return new SimpleModule("viirp-vips-names")
                .setMixInAnnotation(RegistrationCreate.class, RegistrationCreateMixIn.class)
                .setMixInAnnotation(Registration.class, RegistrationMixIn.class)
                .setMixInAnnotation(ProhibitionCreate.class, ProhibitionMixIn.class)
                .setMixInAnnotation(Prohibition.class, ProhibitionMixIn.class)
                .setMixInAnnotation(ImpoundmentCreate.class, ImpoundmentMixIn.class)
                .setMixInAnnotation(Impoundment.class, ImpoundmentMixIn.class)
                .setMixInAnnotation(Document.class, DocumentMixIn.class)
                .setMixInAnnotation(CodeTables.class, CodeTablesMixIn.class)
                .setMixInAnnotation(CodeDescription.class, CodeDescriptionMixIn.class);
    }

    /** Writes {@code true}/{@code false} as {@code "Y"}/{@code "N"}. */
    public static final class YesNoSerializer extends StdScalarSerializer<Boolean> {

        public YesNoSerializer() {
            super(Boolean.class);
        }

        @Override
        public void serialize(Boolean value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(value ? "Y" : "N");
        }
    }

    /** Reads exactly {@code "Y"} or {@code "N"}; any other value is an invalid VIPS response. */
    public static final class YesNoDeserializer extends StdScalarDeserializer<Boolean> {

        private static final String MESSAGE = "Must be \"Y\" or \"N\".";

        public YesNoDeserializer() {
            super(Boolean.class);
        }

        @Override
        public Boolean deserialize(JsonParser p, DeserializationContext ctxt) {
            if (!p.hasToken(JsonToken.VALUE_STRING)) {
                throw MismatchedInputException.from(p, Boolean.class, MESSAGE);
            }
            return switch (p.getString()) {
                case "Y" -> Boolean.TRUE;
                case "N" -> Boolean.FALSE;
                default -> throw InvalidFormatException.from(p, MESSAGE, p.getString(), Boolean.class);
            };
        }
    }

    private interface RegistrationCreateMixIn {
        @JsonProperty("vipsAddressArray") List<AddressCreate> getAddresses();
        @JsonProperty("vipsAddressArray") void setAddresses(List<AddressCreate> value);
        @JsonProperty("vipsLicenceObj") LicenceCreate getLicence();
        @JsonProperty("vipsLicenceObj") void setLicence(LicenceCreate value);
    }

    private interface RegistrationMixIn {
        @JsonProperty("vipsAddressArray") List<Address> getAddresses();
        @JsonProperty("vipsAddressArray") void setAddresses(List<Address> value);
        @JsonProperty("vipsLicenceObj") Licence getLicence();
        @JsonProperty("vipsLicenceObj") void setLicence(Licence value);
    }

    /** {@link ProhibitionCreate} and {@link Prohibition}. */
    private interface ProhibitionMixIn {
        @JsonProperty("policeDetatchmentId") Long getPoliceDetachmentId();
        @JsonProperty("policeDetatchmentId") void setPoliceDetachmentId(Long value);
        @JsonProperty("driverLicenceSeizedYN") @JsonSerialize(using = YesNoSerializer.class) Boolean getDriverLicenceSeized();
        @JsonProperty("driverLicenceSeizedYN") @JsonDeserialize(using = YesNoDeserializer.class) void setDriverLicenceSeized(Boolean value);
    }

    /** {@link ImpoundmentCreate} and {@link Impoundment}. */
    private interface ImpoundmentMixIn {
        @JsonProperty("policeDetatchmentId") Long getPoliceDetachmentId();
        @JsonProperty("policeDetatchmentId") void setPoliceDetachmentId(Long value);
    }

    private interface DocumentMixIn {
        @JsonProperty("disclosureDocumentYN") @JsonSerialize(using = YesNoSerializer.class) Boolean getDisclosureDocument();
        @JsonProperty("disclosureDocumentYN") @JsonDeserialize(using = YesNoDeserializer.class) void setDisclosureDocument(Boolean value);
    }

    private interface CodeTablesMixIn {
        @JsonProperty("data_sources") List<CodeDescription> getDataSources();
        @JsonProperty("data_sources") void setDataSources(List<CodeDescription> value);
        @JsonProperty("registration_roles") List<CodeDescription> getRegistrationRoles();
        @JsonProperty("registration_roles") void setRegistrationRoles(List<CodeDescription> value);
    }

    private interface CodeDescriptionMixIn {
        @JsonProperty("internalYN") @JsonSerialize(using = YesNoSerializer.class) Boolean getInternal();
        @JsonProperty("internalYN") @JsonDeserialize(using = YesNoDeserializer.class) void setInternal(Boolean value);
    }

}
