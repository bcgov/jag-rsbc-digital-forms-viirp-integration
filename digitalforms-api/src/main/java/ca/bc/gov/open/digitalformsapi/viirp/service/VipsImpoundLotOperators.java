package ca.bc.gov.open.digitalformsapi.viirp.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.bc.gov.open.digitalformsapi.viirp.exception.UpstreamServiceException;
import ca.bc.gov.open.digitalformsapi.viirp.model.ImpoundLotOperator;
import tools.jackson.databind.JsonNode;

final class VipsImpoundLotOperators {

    private VipsImpoundLotOperators() {
    }

    static List<ImpoundLotOperator> fromVips(JsonNode entries) {
        List<ImpoundLotOperator> operators = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        if (entries == null || !entries.isArray()) {
            throw invalidResponse();
        }
        for (JsonNode entry : entries) {
            long id = number(entry, "identityId");
            if (id <= 0 || !ids.add(id)) {
                throw invalidResponse();
            }
            operators.add(mapOperator(entry, id));
        }
        return operators;
    }

    private static ImpoundLotOperator mapOperator(JsonNode entry, long id) {
        ImpoundLotOperator operator = new ImpoundLotOperator(id, new ArrayList<>(), new ArrayList<>());
        operator.setLegalNm(text(entry, "legalNm"));
        operator.setBusinessNm(text(entry, "businessNm"));
        operator.setAddressFirstLineTxt(text(entry, "addressFirstLineTxt"));
        operator.setAddressSecondLineTxt(text(entry, "addressSecondLineTxt"));
        operator.setAddressThirdLineTxt(text(entry, "addressThirdLineTxt"));
        operator.setCityNm(text(entry, "cityNm"));
        operator.setProvinceCd(text(entry, "provinceCd"));
        operator.setPostalCodeTxt(text(entry, "postalCodeTxt"));
        for (String phone : numbers(entry, "phoneNumbers")) {
            operator.addPhoneNumbersItem(phone);
        }
        for (String fax : numbers(entry, "faxNumbers")) {
            operator.addFaxNumbersItem(fax);
        }
        return operator;
    }

    private static List<String> numbers(JsonNode entry, String name) {
        JsonNode values = entry.path(name);
        if (!values.isArray()) {
            throw invalidResponse();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isString() || value.asString().isBlank()) {
                throw invalidResponse();
            }
            result.add(value.asString());
        }
        return result;
    }

    private static long number(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw invalidResponse();
        }
        return value.longValue();
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            throw invalidResponse();
        }
        return value.asString();
    }

    private static UpstreamServiceException invalidResponse() {
        return UpstreamServiceException.invalidResponse("VIPS returned invalid impound-lot operators.");
    }
}
