package ca.bc.gov.open.digitalformsapi.viirp.service;

import java.util.List;
import java.util.Map;

import ca.bc.gov.open.digitalformsapi.viirp.exception.UpstreamServiceException;
import ca.bc.gov.open.digitalformsapi.viirp.model.CodeTablesResponse;
import ca.bc.gov.open.digitalformsapi.viirp.utils.VipsJson;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

final class VipsCodeTables {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .addModule(VipsJson.module())
            .build();

    private static final List<String> TABLES = List.of(
            "addresses", "contactMethods", "countries", "data_sources", "disposalActs", "disposalDecisions",
            "driverLicenceOffices", "jurisdictions", "noticeTypes", "policeDetachments",
            "provinces", "registration_roles", "releaseReasons", "reviewStatuses", "scheduleAppTypes",
            "unavailabilityReasons", "vehicleTypes");

    private static final Map<String, String> GROUPED_TABLES = Map.ofEntries(
            Map.entry("decisionOutcomes", "noticeActiveYN"),
            Map.entry("documentNotices", "documentActiveYN"),
            Map.entry("documents", "noticeActiveYN"),
            Map.entry("dreEvaluations", "noticeActiveYN"),
            Map.entry("groundsDecisions", "reviewAppActiveYN"),
            Map.entry("noticePrefixNos", "noticeActiveYN"),
            Map.entry("originalCauses", "noticeActiveYN"),
            Map.entry("reviewApplications", "noticeActiveYN"),
            Map.entry("reviewRoles", "noticeActiveYN"),
            Map.entry("reviewTypes", "noticeActiveYN"));

    private VipsCodeTables() {
    }

    static CodeTablesResponse fromVips(JsonNode response) {
        JsonNode resultCode = response.path("respCd");
        if (!resultCode.isIntegralNumber() || !resultCode.canConvertToLong() || resultCode.longValue() != 0) {
            throw UpstreamServiceException.invalidResponse("VIPS returned a failed code-table response.");
        }
        JsonNode configuration = response.path("configuration");
        if (!(configuration instanceof ObjectNode object)) {
            throw UpstreamServiceException.invalidResponse("VIPS returned no code-table configuration.");
        }
        object.remove("impoundLotOperators");
        var operators = VipsImpoundLotOperators.fromVips(object.remove("impoundLotOperatorDetails"));
        TABLES.forEach(table -> filter(object, table, "activeYN", false));
        GROUPED_TABLES.forEach((table, flag) -> filter(object, table, flag, true));
        try {
            CodeTablesResponse result = MAPPER.treeToValue(response, CodeTablesResponse.class);
            result.getConfiguration().setImpoundLotOperators(operators);
            return result;
        } catch (JacksonException ex) {
            throw UpstreamServiceException.invalidResponse("VIPS returned invalid code-table configuration.", ex);
        }
    }

    private static void filter(ObjectNode parent, String property, String flag, boolean grouped) {
        JsonNode values = parent.get(property);
        if (values == null || values.isNull()) {
            return;
        }
        if (!values.isArray()) {
            throw UpstreamServiceException.invalidResponse("VIPS returned an invalid code-table array: " + property);
        }
        ArrayNode active = MAPPER.createArrayNode();
        for (JsonNode value : values) {
            if (value.isNull()) {
                continue;
            }
            if (!(value instanceof ObjectNode entry)) {
                throw UpstreamServiceException.invalidResponse("VIPS returned an invalid code-table entry: " + property);
            }
            if (isActive(entry.remove(flag), property)) {
                if (grouped) {
                    filter(entry, "codes", "activeYN", false);
                }
                active.add(entry);
            }
        }
        parent.set(property, active);
    }

    private static boolean isActive(JsonNode flag, String table) {
        if (flag == null || flag.isNull()) {
            return false;
        }
        if (flag.isString()) {
            return switch (flag.asString()) {
                case "Y" -> true;
                case "N" -> false;
                default -> throw UpstreamServiceException.invalidResponse(
                        "VIPS returned an invalid activity flag for code table: " + table);
            };
        }
        throw UpstreamServiceException.invalidResponse("VIPS returned an invalid activity flag for code table: " + table);
    }
}
