package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;

class ProhibitionsHttpTests extends HttpApiTestSupport {

    private static final String NOTICE = "21900601";
    static final String BODY = """
            {
              "prohibition": {
                "prohibitionNoticeNo": "21900601",
                "driverLicenceSeized": false,
                "effectiveDt": "2022-10-11",
                "noticeServedDt": "2022-10-11",
                "noticeTypeCd": "IRP",
                "originalCauseCd": "IRPINDEF", "policeDetachmentId": 304,
                "cityNm": "VICTORIA", "expiryDt": null
              },
              "registration": {
                "dataSourceCd": "VIPS", "registrationRoleCd": "REGOWN",
                "surnameNm": "HTTP TEST", "addresses": [],
                "licence": {"driverLicenceNo": "00001234", "dlJurisdictionCd": "BC"}
              }
            }
            """;

    @Test
    void authenticatesCreatesAndReadsThroughVipsHttp() throws Exception {
        String createPath = "/prohibitions";
        String getPath = "/prohibitions/" + NOTICE;
        responseJson(request("POST", createPath, BODY, null), 401);
        responseJson(request("GET", getPath, null, UNLISTED_CLIENT_AUTH), 401);
        assertTrue(vips.requests.isEmpty(), "Unauthorized requests must not reach VIPS");

        availableNotice();
        vips.enqueue(200, """
                {"respCd":0,"respMsg":"created","prohibitionId":123456789012345,"extraVipsMetadata":"ignored"}
                """);
        var created = request("POST", createPath, BODY, AUTH);
        assertEquals(json.readTree("{\"prohibitionId\":123456789012345}"),
                responseJson(created, 201));
        String resourcePath = createdResourcePath(created, getPath);
        checkedNotice("prohibition", NOTICE);
        JsonNode submitted = json.readTree(downstream("POST", "/cases/prohibitions").body());
        assertEquals(NOTICE, submitted.at("/vipsProhibitionCreate/prohibitionNoticeNo").asString());
        assertEquals(json.createArrayNode(), submitted.path("vipsDocumentIdArray"));
        assertEquals("N", submitted.at("/vipsProhibitionCreate/driverLicenceSeizedYN").asString());
        assertEquals("IRP", submitted.at("/vipsProhibitionCreate/noticeTypeCd").asString());
        assertEquals("PERS", submitted.at("/vipsProhibitionCreate/noticeSubjectCd").asString());
        assertEquals("00001234", submitted.at("/vipsRegistrationCreate/vipsLicenceObj/driverLicenceNo").asString());
        assertFalse(submitted.at("/vipsRegistrationCreate").has("vipsLicenceCreateObj"),
                "VIPS WS only binds vipsLicenceObj; any other licence property name is silently dropped");
        assertEquals(304L, submitted.at("/vipsProhibitionCreate/policeDetatchmentId").asLong());
        assertTrue(submitted.at("/vipsRegistrationCreate/vipsAddressArray").isArray());
        assertEquals(Set.of("vipsDocumentIdArray", "vipsProhibitionCreate", "vipsRegistrationCreate"),
                Set.copyOf(submitted.propertyNames()), "VIPS WS binds only its own property names");
        assertFalse(submitted.path("vipsProhibitionCreate").has("driverLicenceSeized"));
        assertFalse(submitted.path("vipsProhibitionCreate").has("policeDetachmentId"));
        assertFalse(submitted.path("vipsRegistrationCreate").has("licence"));
        assertFalse(submitted.path("vipsRegistrationCreate").has("addresses"));
        assertTrue(submitted.at("/vipsProhibitionCreate/expiryDt").isNull());
        assertEquals("2022-10-11T00:00:00.000-07:00", submitted.at("/vipsProhibitionCreate/effectiveDt").asString(),
                "VIPS WS binds dates as timestamps, so a date is sent as B.C. midnight");
        assertTrue(submitted.at("/vipsProhibitionCreate/dreEvaluationCds").isNull(),
                "Omitted optional arrays must be forwarded as null, not []");

        vips.enqueue(200, """
                {"respCd":0,"resultsCount":null,"result":[{"prohibitionId":123456789012345}]}
                """);
        vips.enqueue(200, """
                {"respCd":0,"result":{"prohibitionNoticeNo":"%s","cityNm":"%s",
                 "effectiveDt":"%s","cancelled":false,"extraVipsField":"ignored",
                 "policeDetatchmentId":304,"driverLicenceSeizedYN":"Y","noticeTypeCd":"IRP","noticeSubjectCd":"PERS",
                 "registration":{"surnameNm":"HTTP TEST","vipsLicenceObj":{"driverLicenceNo":"00001234"},
                  "vipsAddressArray":[{"cityNm":"VICTORIA"}]},
                 "documents":[{"documentId":7,"disclosureDocumentYN":"N"}]}}
                """.formatted(NOTICE, submitted.at("/vipsProhibitionCreate/cityNm").asString(),
                        submitted.at("/vipsProhibitionCreate/effectiveDt").asString()));
        JsonNode result = responseJson(request("GET", resourcePath, null, AUTH), 200);
        assertEquals(NOTICE, result.path("prohibitionNoticeNo").asString());
        assertEquals("VICTORIA", result.path("cityNm").asString());
        assertEquals("2022-10-11", result.path("effectiveDt").asString());
        assertEquals(json.readTree("false"), result.path("cancelled"));
        assertEquals(304L, result.path("policeDetachmentId").asLong());
        assertEquals("IRP", result.path("noticeTypeCd").asString());
        assertEquals("PERS", result.path("noticeSubjectCd").asString());
        assertEquals(json.readTree("true"), result.path("driverLicenceSeized"));
        assertEquals("00001234", result.at("/registration/licence/driverLicenceNo").asString());
        assertEquals("VICTORIA", result.at("/registration/addresses/0/cityNm").asString());
        assertEquals(json.readTree("false"), result.at("/documents/0/disclosureDocument"));
        assertFalse(result.has("policeDetatchmentId"));
        assertFalse(result.has("driverLicenceSeizedYN"));
        assertFalse(result.path("registration").has("vipsLicenceObj"));
        assertFalse(result.path("registration").has("vipsAddressArray"));
        assertFalse(result.at("/documents/0").has("disclosureDocumentYN"));
        assertFalse(result.has("result"));
        assertFalse(result.has("respCd"));
        assertFalse(result.has("respMsg"));
        assertFalse(result.has("extraVipsField"));
        downstream("GET", "/cases/prohibitions/search?prohibitionNoticeNo=" + NOTICE);
        downstream("GET", "/cases/prohibitions/123456789012345");
    }

    @Test
    void missingNoticeReturns404WithoutFetchingDetails() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[]}");
        JsonNode error = responseJson(
                request("GET", "/prohibitions/" + NOTICE, null, AUTH), 404);
        assertEquals("Not Found", error.path("detail").asString());
        downstream("GET", "/cases/prohibitions/search?prohibitionNoticeNo=" + NOTICE);
    }

    @Test
    void vipsTimestampsAreReadAsTheirCalendarDateWithoutAZoneShift() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"prohibitionId\":123456789012345}]}");
        vips.enqueue(200, """
                {"respCd":0,"result":{"prohibitionNoticeNo":"%s",
                 "effectiveDt":"2025-12-15T00:00:00.000-08:00",
                 "expiryDt":"2025-12-15T00:00:00.000-07:00",
                 "noticeServedDt":"2026-12-15T00:00:00.000-07:00",
                 "tempDlEffectiveDt":"2026-11-05",
                 "tempDlExpiryDt":null,
                 "incidentDtm":"2025-12-15T23:30:00.000-08:00",
                 "fieldAddedByVips":"ignored"}}
                """.formatted(NOTICE));
        JsonNode result = responseJson(request("GET", "/prohibitions/" + NOTICE, null, AUTH), 200);
        assertEquals("2025-12-15", result.path("effectiveDt").asString());
        assertEquals("2025-12-15", result.path("expiryDt").asString(),
                "A VIPS server on a fixed -07:00 offset must not move a winter date to the previous day");
        assertEquals("2026-12-15", result.path("noticeServedDt").asString());
        assertEquals("2026-11-05", result.path("tempDlEffectiveDt").asString());
        assertTrue(result.path("tempDlExpiryDt").isNull());
        assertEquals("2025-12-15T23:30:00.000-08:00", result.path("incidentDtm").asString(),
                "Timestamps keep the instant and offset VIPS returned");
        assertFalse(result.has("fieldAddedByVips"), "The VIPS codec must still ignore unknown fields");
        downstream("GET", "/cases/prohibitions/search?prohibitionNoticeNo=" + NOTICE);
        downstream("GET", "/cases/prohibitions/123456789012345");
    }

    @Test
    void malformedVipsDateIsAnInvalidUpstreamResponse() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"prohibitionId\":123456789012345}]}");
        vips.enqueue(200, """
                {"respCd":0,"result":{"prohibitionNoticeNo":"%s","effectiveDt":"15/12/2025"}}
                """.formatted(NOTICE));
        JsonNode error = responseJson(request("GET", "/prohibitions/" + NOTICE, null, AUTH), 502);
        assertEquals("Invalid response from VIPS.", error.path("detail").asString());
        assertFalse(error.toString().contains("15/12/2025"));
        downstream("GET", "/cases/prohibitions/search?prohibitionNoticeNo=" + NOTICE);
        downstream("GET", "/cases/prohibitions/123456789012345");
    }

    @Test
    void seizedLicenceIsSentToVipsAsY() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":0,\"prohibitionId\":123456789012345}");
        responseJson(request("POST", "/prohibitions",
                BODY.replace("\"driverLicenceSeized\": false", "\"driverLicenceSeized\": true"), AUTH), 201);
        checkedNotice("prohibition", NOTICE);
        JsonNode submitted = json.readTree(downstream("POST", "/cases/prohibitions").body());
        assertEquals("Y", submitted.at("/vipsProhibitionCreate/driverLicenceSeizedYN").asString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"Y\"", "\"N\"", "\"true\"", "\"\"", "1", "0", "{}", "[]", "[true]"})
    void driverLicenceSeizedMustBeAJsonBoolean(String value) throws Exception {
        JsonNode error = responseJson(request("POST", "/prohibitions",
                BODY.replace("\"driverLicenceSeized\": false", "\"driverLicenceSeized\": " + value), AUTH), 400);
        assertEquals("Request validation failed.", error.path("detail").asString());
        assertEquals(json.readTree("""
                [{"field":"prohibition.driverLicenceSeized","message":"Must be true or false."}]
                """), error.path("errors"));
        assertTrue(vips.requests.isEmpty(), "Invalid requests must not reach VIPS");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"X\"", "\"y\"", "\"\"", "true", "1"})
    void invalidVipsYesNoFlagIsAnInvalidUpstreamResponse(String flag) throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"prohibitionId\":123456789012345}]}");
        vips.enqueue(200, """
                {"respCd":0,"result":{"prohibitionNoticeNo":"%s","driverLicenceSeizedYN":%s}}
                """.formatted(NOTICE, flag));
        JsonNode error = responseJson(request("GET", "/prohibitions/" + NOTICE, null, AUTH), 502);
        assertEquals("Invalid response from VIPS.", error.path("detail").asString());
        downstream("GET", "/cases/prohibitions/search?prohibitionNoticeNo=" + NOTICE);
        downstream("GET", "/cases/prohibitions/123456789012345");
    }

    @Test
    void legacyVipsPropertyNamesAreNotAcceptedAsAliases() throws Exception {
        String legacy = BODY.replace("\"prohibition\"", "\"vipsProhibitionCreate\"")
                .replace("\"registration\"", "\"vipsRegistrationCreate\"");
        JsonNode error = responseJson(request("POST", "/prohibitions", legacy, AUTH), 400);
        Set<String> fields = new HashSet<>();
        error.path("errors").forEach(validation -> fields.add(validation.path("field").asString()));
        assertEquals(Set.of("prohibition", "registration"), fields, error.toString());
        assertTrue(vips.requests.isEmpty(), "Invalid requests must not reach VIPS");
    }

    @Test
    void vipsCreateFailureIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":1,\"respMsg\":\"Rejected\"}");
        availableNotice();
        JsonNode error = responseJson(request("POST", "/prohibitions", BODY, AUTH), 502);
        assertEquals("Invalid response from VIPS.", error.path("detail").asString());
        assertFalse(error.has("respMsg"));
        checkedNotice("prohibition", NOTICE);
        downstream("POST", "/cases/prohibitions");
        checkedNotice("prohibition", NOTICE);
    }

    @Test
    void missingVipsProhibitionIdIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":0,\"respMsg\":\"created\"}");
        JsonNode error = responseJson(request("POST", "/prohibitions", BODY, AUTH), 502);
        assertEquals("Invalid response from VIPS.", error.path("detail").asString());
        checkedNotice("prohibition", NOTICE);
        downstream("POST", "/cases/prohibitions");
    }

    @Test
    void savedNoticesReturnOnlyTheirIdWithoutUpstreamWarnings() throws Exception {
        String createPath = "/prohibitions";
        availableNotice();
        vips.enqueue(200, "{\"respCd\":11,\"respMsg\":\"Update Drivers manually\",\"prohibitionId\":123456789012345}");
        var manualUpdate = request("POST", createPath, BODY, AUTH);
        assertEquals(json.readTree("{\"prohibitionId\":123456789012345}"),
                responseJson(manualUpdate, 201));
        createdResourcePath(manualUpdate, "/prohibitions/" + NOTICE);
        checkedNotice("prohibition", NOTICE);
        downstream("POST", "/cases/prohibitions");

        availableNotice();
        vips.enqueue(200, "{\"respCd\":29,\"respMsg\":\"ICBC post execute failed\",\"prohibitionId\":123456789012345}");
        var icbcFailure = request("POST", createPath, BODY, AUTH);
        assertEquals(json.readTree("{\"prohibitionId\":123456789012345}"),
                responseJson(icbcFailure, 201));
        createdResourcePath(icbcFailure, "/prohibitions/" + NOTICE);
        checkedNotice("prohibition", NOTICE);
        downstream("POST", "/cases/prohibitions");
    }

    @Test
    void unexpectedVipsCreateCodeIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":21,\"respMsg\":\"Unexpected\"}");
        JsonNode error = responseJson(request("POST", "/prohibitions", BODY, AUTH), 502);
        assertEquals("Invalid response from VIPS.", error.path("detail").asString());
        checkedNotice("prohibition", NOTICE);
        downstream("POST", "/cases/prohibitions");
    }

    @Test
    void prohibitionCityLongerThanVipsColumnIsRejectedBeforeCallingVips() throws Exception {
        String tooLongCity = "\"" + "X".repeat(31) + "\"";
        int status = request("POST", "/prohibitions",
                BODY.replace("\"VICTORIA\"", tooLongCity), AUTH).statusCode();
        assertEquals(400, status, "31-character cityNm must be rejected; VIPS city_nm is VARCHAR2(30)");
        assertTrue(vips.requests.isEmpty(), "Invalid requests must not reach VIPS");
    }

}
