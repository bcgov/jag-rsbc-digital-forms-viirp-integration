package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

class ImpoundmentsHttpTests extends HttpApiTestSupport {

    private static final String NOTICE = "00123456";
    static final String BODY = """
            {
              "impoundment": {
                "impoundmentNoticeNo": "00123456", "dlJurisdictionCd": "BC",
                "impoundmentDt": "2022-10-11",
                "originalCauseCds": ["STUNT"], "policeDetachmentId": 304, "policeFileNo": "HTTP-TEST",
                "judicialOrder": false
              },
              "registrations": [{
                "dataSourceCd": "VIPS", "registrationRoleCd": "REGOWN",
                "surnameNm": "HTTP TEST", "addresses": [],
                "licence": {"driverLicenceNo": "00001234", "dlJurisdictionCd": "BC"}
              }],
              "vehicle": {"registrationNo": "00005678", "licencePlateNo": "TEST"}
            }
            """;

    @Test
    void authenticatesCreatesAndReadsThroughVipsHttp() throws Exception {
        String createPath = "/impoundments";
        String getPath = "/impoundments/" + NOTICE;
        responseJson(request("POST", createPath, BODY, null), 401);
        responseJson(request("GET", getPath, null, UNLISTED_CLIENT_AUTH), 401);
        assertTrue(vips.requests.isEmpty(), "Unauthorized requests must not reach VIPS");

        availableNotice();
        vips.enqueue(200, "{\"respCd\":0,\"respMsg\":\"created\",\"impoundmentId\":123456789012345,\"extraVipsMetadata\":\"ignored\"}");
        var created = request("POST", createPath, BODY, AUTH);
        assertEquals(json.readTree("{\"impoundmentId\":123456789012345}"),
                responseJson(created, 201));
        String resourcePath = createdResourcePath(created, getPath);
        checkedNotice("impoundment", NOTICE);
        JsonNode submitted = json.readTree(downstream("POST", "/cases/impoundments").body());
        assertEquals(NOTICE, submitted.at("/vipsImpoundCreate/impoundmentNoticeNo").asString());
        assertEquals(json.createArrayNode(), submitted.path("vipsDocumentIdArray"));
        assertEquals(json.readTree("[\"STUNT\"]"), submitted.at("/vipsImpoundCreate/originalCauseCds"));
        assertEquals("00001234", submitted.at("/vipsRegistrationCreateArray/0/vipsLicenceObj/driverLicenceNo").asString());
        assertFalse(submitted.at("/vipsRegistrationCreateArray/0").has("vipsLicenceCreateObj"),
                "VIPS WS only binds vipsLicenceObj; any other licence property name is silently dropped");
        assertEquals("00005678", submitted.at("/vipsVehicleCreate/registrationNo").asString());
        assertEquals(304L, submitted.at("/vipsImpoundCreate/policeDetatchmentId").asLong());
        assertEquals(json.createArrayNode(), submitted.path("vipsImpoundmentArray"));
        assertEquals("IMP", submitted.at("/vipsImpoundCreate/noticeTypeCd").asString());
        assertEquals("VEHI", submitted.at("/vipsImpoundCreate/noticeSubjectCd").asString());
        assertTrue(submitted.at("/vipsRegistrationCreateArray/0/vipsAddressArray").isArray());
        assertEquals(Set.of("vipsDocumentIdArray", "vipsImpoundCreate", "vipsImpoundmentArray",
                "vipsRegistrationCreateArray", "vipsVehicleCreate"),
                Set.copyOf(submitted.propertyNames()), "VIPS WS binds only its own property names");
        assertFalse(submitted.path("vipsImpoundCreate").has("policeDetachmentId"));
        assertFalse(submitted.at("/vipsRegistrationCreateArray/0").has("licence"));
        assertFalse(submitted.at("/vipsRegistrationCreateArray/0").has("addresses"));
        assertEquals("2022-10-11T00:00:00.000-07:00", submitted.at("/vipsImpoundCreate/impoundmentDt").asString());

        vips.enqueue(200, "{\"respCd\":0,\"resultsCount\":null,\"result\":[{\"impoundmentId\":123456789012345}]}");
        vips.enqueue(200, """
                {"respCd":0,"result":{"impoundmentNoticeNo":"%s","dlJurisdictionCd":"BC",
                 "impoundmentDt":"%s","extraVipsField":"ignored","policeDetatchmentId":304,
                 "judicialOrderYN":"N","fvpa":false,"cancelled":true,"noticeTypeCd":"IMP","noticeSubjectCd":"VEHI",
                 "registrations":[{"surnameNm":"HTTP TEST","vipsLicenceObj":{"driverLicenceNo":"00001234"},
                  "vipsAddressArray":[{"cityNm":"VICTORIA"}]}],
                 "documents":[{"documentId":7,"disclosureDocumentYN":"Y"}]}}
                """.formatted(NOTICE, submitted.at("/vipsImpoundCreate/impoundmentDt").asString()));
        JsonNode result = responseJson(request("GET", resourcePath, null, AUTH), 200);
        assertEquals(NOTICE, result.path("impoundmentNoticeNo").asString());
        assertEquals("BC", result.path("dlJurisdictionCd").asString());
        assertEquals("2022-10-11", result.path("impoundmentDt").asString());
        assertEquals(304L, result.path("policeDetachmentId").asLong());
        assertEquals("IMP", result.path("noticeTypeCd").asString());
        assertEquals("VEHI", result.path("noticeSubjectCd").asString());
        assertEquals(json.readTree("true"), result.path("cancelled"));
        assertEquals("00001234", result.at("/registrations/0/licence/driverLicenceNo").asString());
        assertEquals("VICTORIA", result.at("/registrations/0/addresses/0/cityNm").asString());
        assertEquals(json.readTree("true"), result.at("/documents/0/disclosureDocument"));
        assertFalse(result.has("policeDetatchmentId"));
        assertFalse(result.has("judicialOrderYN"));
        assertFalse(result.at("/registrations/0").has("vipsLicenceObj"));
        assertFalse(result.at("/documents/0").has("disclosureDocumentYN"));
        assertFalse(result.has("result"));
        assertFalse(result.has("respCd"));
        assertFalse(result.has("respMsg"));
        assertFalse(result.has("extraVipsField"));
        downstream("GET", "/cases/impoundments/search?impoundmentNoticeNo=" + NOTICE);
        downstream("GET", "/cases/impoundments/123456789012345");
    }

    @Test
    void missingNoticeReturns404WithoutFetchingDetails() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[]}");
        JsonNode error = responseJson(
                request("GET", "/impoundments/" + NOTICE, null, AUTH), 404);
        assertEquals("Not Found", error.path("detail").asString());
        downstream("GET", "/cases/impoundments/search?impoundmentNoticeNo=" + NOTICE);
    }

    @Test
    void vipsCreateFailureIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":99,\"respMsg\":\"Rejected\"}");
        JsonNode error = responseJson(request("POST", "/impoundments", BODY, AUTH), 502);
        assertEquals("Invalid response from VIPS.", error.path("detail").asString());
        assertFalse(error.has("respMsg"));
        checkedNotice("impoundment", NOTICE);
        downstream("POST", "/cases/impoundments");
    }

    @Test
    void missingVipsImpoundmentIdIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":0,\"respMsg\":\"created\"}");
        JsonNode error = responseJson(request("POST", "/impoundments", BODY, AUTH), 502);
        assertEquals("Invalid response from VIPS.", error.path("detail").asString());
        checkedNotice("impoundment", NOTICE);
        downstream("POST", "/cases/impoundments");
    }

    @Test
    void savedNoticesReturnOnlyTheirIdWithoutUpstreamWarnings() throws Exception {
        String createPath = "/impoundments";
        availableNotice();
        vips.enqueue(200, "{\"respCd\":29,\"respMsg\":\"ICBC post execute failed\",\"impoundmentId\":123456789012345}");
        var icbcFailure = request("POST", createPath, BODY, AUTH);
        assertEquals(json.readTree("{\"impoundmentId\":123456789012345}"),
                responseJson(icbcFailure, 201));
        createdResourcePath(icbcFailure, "/impoundments/" + NOTICE);
        checkedNotice("impoundment", NOTICE);
        downstream("POST", "/cases/impoundments");

        availableNotice();
        vips.enqueue(200, "{\"respCd\":11,\"respMsg\":\"Update Drivers manually\",\"impoundmentId\":123456789012345}");
        var manualUpdate = request("POST", createPath, BODY, AUTH);
        assertEquals(json.readTree("{\"impoundmentId\":123456789012345}"),
                responseJson(manualUpdate, 201));
        createdResourcePath(manualUpdate, "/impoundments/" + NOTICE);
        checkedNotice("impoundment", NOTICE);
        downstream("POST", "/cases/impoundments");
    }

    @Test
    void unexpectedVipsCreateCodeIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":21,\"respMsg\":\"Unexpected\"}");
        JsonNode error = responseJson(request("POST", "/impoundments", BODY, AUTH), 502);
        assertEquals("Invalid response from VIPS.", error.path("detail").asString());
        checkedNotice("impoundment", NOTICE);
        downstream("POST", "/cases/impoundments");
    }

    @Test
    void missingDriverLicenceIsRejectedBeforeCallingVips() throws Exception {
        JsonNode error = responseJson(request("POST", "/impoundments",
                BODY.replace("\"00001234\"", "\"\""), AUTH), 400);
        assertEquals("registrations[0].licence.driverLicenceNo",
                error.at("/errors/0/field").asString());
        assertEquals("Invalid license object - DL is a required field.", error.at("/errors/0/message").asString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void judicialOrderIsForwardedAsVipsYesOrNo(boolean value) throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":0,\"impoundmentId\":123}");
        String body = BODY.replace("\"STUNT\"", "\"FLTPOL\"")
                .replace("\"judicialOrder\": false", "\"judicialOrder\": " + value);
        responseJson(request("POST", "/impoundments", body, AUTH), 201);
        checkedNotice("impoundment", NOTICE);
        JsonNode submitted = json.readTree(downstream("POST", "/cases/impoundments").body());
        assertEquals(value ? "Y" : "N", submitted.at("/vipsImpoundCreate/judicialOrderYN").asString());
        assertFalse(submitted.path("vipsImpoundCreate").has("judicialOrder"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void fvpaCauseRequiresJudicialOrderBeforeCreating(boolean explicitNull) throws Exception {
        causesConfiguration();
        String body = (explicitNull ? BODY.replace("\"judicialOrder\": false", "\"judicialOrder\": null")
                : withoutJudicialOrder()).replace("[\"STUNT\"]", "[\"STUNT\", \"FLTPOL\"]");
        JsonNode error = responseJson(request("POST", "/impoundments", body, AUTH), 400);
        assertEquals("impoundment.judicialOrder", error.at("/errors/0/field").asString());
        downstream("GET", "/configuration");
    }

    @Test
    void nonFvpaCauseMayOmitJudicialOrder() throws Exception {
        causesConfiguration();
        availableNotice();
        vips.enqueue(200, "{\"respCd\":0,\"impoundmentId\":123}");
        responseJson(request("POST", "/impoundments", withoutJudicialOrder(), AUTH), 201);
        downstream("GET", "/configuration");
        checkedNotice("impoundment", NOTICE);
        JsonNode value = json.readTree(downstream("POST", "/cases/impoundments").body())
                .at("/vipsImpoundCreate/judicialOrderYN");
        assertTrue(value.isMissingNode() || value.isNull());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"true\"", "1", "[]"})
    void judicialOrderMustBeAJsonBoolean(String value) throws Exception {
        JsonNode error = responseJson(request("POST", "/impoundments",
                BODY.replace("\"judicialOrder\": false", "\"judicialOrder\": " + value), AUTH), 400);
        assertEquals("impoundment.judicialOrder", error.at("/errors/0/field").asString());
    }

    @Test
    void unknownCauseCannotBypassFvpaValidation() throws Exception {
        causesConfiguration();
        JsonNode error = responseJson(request("POST", "/impoundments",
                withoutJudicialOrder().replace("STUNT", "UNKNOWN"), AUTH), 400);
        assertEquals("impoundment.originalCauseCds", error.at("/errors/0/field").asString());
        downstream("GET", "/configuration");
    }

    @Test
    void malformedCauseConfigurationCannotBypassFvpaValidation() throws Exception {
        vips.enqueue(200, """
                {"respCd":0,"configuration":{"originalCauses":[
                  {"noticeTypeCd":"IMP","codes":[{"objectCd":"STUNT"}]}]}}
                """);
        responseJson(request("POST", "/impoundments", withoutJudicialOrder(), AUTH), 502);
        downstream("GET", "/configuration");
    }

    @Test
    void unavailableConfigurationPreventsCreation() throws Exception {
        vips.enqueue(503, "{}");
        responseJson(request("POST", "/impoundments", withoutJudicialOrder(), AUTH), 503);
        downstream("GET", "/configuration");
    }

    private String withoutJudicialOrder() {
        return BODY.replace(",\n    \"judicialOrder\": false", "");
    }

    private void causesConfiguration() {
        vips.enqueue(200, """
                {"respCd":0,"configuration":{"originalCauses":[
                  {"noticeTypeCd":"IMP","noticeActiveYN":"Y","codes":[
                    {"objectCd":"STUNT","objectType":"MV","activeYN":"Y"},
                    {"objectCd":"FLTPOL","objectType":"FVPA","activeYN":"Y"}]}]}}
                """);
    }
}
