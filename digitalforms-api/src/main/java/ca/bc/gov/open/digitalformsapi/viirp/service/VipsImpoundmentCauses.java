package ca.bc.gov.open.digitalformsapi.viirp.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.open.digitalformsapi.viirp.exception.BadRequestException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.UpstreamServiceException;
import tools.jackson.databind.JsonNode;

final class VipsImpoundmentCauses {

    private VipsImpoundmentCauses() {
    }

    static boolean hasFvpa(JsonNode response, List<String> requestedCauses) {
        JsonNode resultCode = response.path("respCd");
        JsonNode groups = response.path("configuration").path("originalCauses");
        if (!resultCode.isIntegralNumber() || !resultCode.canConvertToLong()
                || resultCode.longValue() != 0 || !groups.isArray()) {
            throw invalidConfiguration();
        }

        Map<String, String> types = new HashMap<>();
        boolean foundImpoundmentGroup = false;
        for (JsonNode group : groups) {
            if (!"IMP".equals(group.path("noticeTypeCd").asString())) {
                continue;
            }
            foundImpoundmentGroup = true;
            JsonNode codes = group.path("codes");
            if (!codes.isArray()) {
                throw invalidConfiguration();
            }
            for (JsonNode entry : codes) {
                JsonNode code = entry.path("objectCd");
                if (!code.isString() || code.asString().isBlank()) {
                    throw invalidConfiguration();
                }
                if (requestedCauses.contains(code.asString())) {
                    JsonNode type = entry.path("objectType");
                    if (!type.isString() || !List.of("FVPA", "MV").contains(type.asString())) {
                        throw invalidConfiguration();
                    }
                    String previous = types.putIfAbsent(code.asString(), type.asString());
                    if (previous != null && !previous.equals(type.asString())) {
                        throw invalidConfiguration();
                    }
                }
            }
        }
        if (!foundImpoundmentGroup) {
            throw invalidConfiguration();
        }
        boolean hasFvpa = false;
        for (String code : requestedCauses) {
            String type = types.get(code);
            if (type == null) {
                throw new BadRequestException("impoundment.originalCauseCds",
                        "Cannot determine whether judicialOrder is required for an original cause; "
                                + "correct the cause or supply judicialOrder.");
            }
            hasFvpa |= "FVPA".equals(type);
        }
        return hasFvpa;
    }

    private static UpstreamServiceException invalidConfiguration() {
        return UpstreamServiceException.invalidResponse("VIPS returned invalid impoundment original causes.");
    }
}
