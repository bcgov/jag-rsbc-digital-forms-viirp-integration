package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class ProhibitionsHttpTests extends HttpApiTestSupport {

    private static final String NOTICE = "21900601";
    static final String BODY = """
            {
              "vipsDocumentIdArray": [123456789012345],
              "vipsProhibitionCreate": {
                "prohibitionNoticeNo": "21900601",
                "driverLicenceSeizedYN": "N",
                "effectiveDt": "2022-10-11T00:00:00.000-07:00",
                "noticeServedDt": "2022-10-11T00:00:00.000-07:00",
                "noticeSubjectCd": "PERS", "noticeTypeCd": "IRP",
                "originalCauseCd": "IRPINDEF", "policeDetatchmentId": 304,
                "cityNm": "VICTORIA", "expiryDt": null
              },
              "vipsRegistrationCreate": {
                "dataSourceCd": "VIPS", "registrationRoleCd": "REGOWN",
                "surnameNm": "HTTP TEST", "vipsAddressArray": [],
                "vipsLicenceObj": {"driverLicenceNo": "00001234", "dlJurisdictionCd": "BC"}
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
        assertEquals(json.readTree("{\"respMsg\":\"success\",\"prohibitionId\":123456789012345}"),
                responseJson(request("POST", createPath, BODY, AUTH), 201));
        checkedNotice("prohibition", NOTICE);
        JsonNode submitted = json.readTree(downstream("POST", "/cases/prohibitions").body());
        assertEquals(NOTICE, submitted.at("/vipsProhibitionCreate/prohibitionNoticeNo").asString());
        assertEquals(123456789012345L, submitted.at("/vipsDocumentIdArray/0").asLong());
        assertEquals("N", submitted.at("/vipsProhibitionCreate/driverLicenceSeizedYN").asString());
        assertEquals("00001234", submitted.at("/vipsRegistrationCreate/vipsLicenceObj/driverLicenceNo").asString());
        assertFalse(submitted.at("/vipsRegistrationCreate").has("vipsLicenceCreateObj"),
                "VIPS WS only binds vipsLicenceObj; any other licence property name is silently dropped");
        assertTrue(submitted.at("/vipsProhibitionCreate/expiryDt").isNull());
        assertTrue(submitted.at("/vipsProhibitionCreate/dreEvaluationCds").isNull(),
                "Omitted optional arrays must be forwarded as null, not []");

        vips.enqueue(200, """
                {"respCd":0,"resultsCount":null,"result":[{"prohibitionId":123456789012345}]}
                """);
        vips.enqueue(200, """
                {"respCd":0,"result":{"prohibitionNoticeNo":"%s","cityNm":"%s",
                 "effectiveDt":"%s","cancelled":false,"extraVipsField":"ignored"}}
                """.formatted(NOTICE, submitted.at("/vipsProhibitionCreate/cityNm").asString(),
                        submitted.at("/vipsProhibitionCreate/effectiveDt").asString()));
        JsonNode result = responseJson(request("GET", getPath, null, AUTH), 200);
        assertEquals(NOTICE, result.at("/result/prohibitionNoticeNo").asString());
        assertEquals("VICTORIA", result.at("/result/cityNm").asString());
        assertEquals("2022-10-11T00:00:00.000-07:00", result.at("/result/effectiveDt").asString());
        assertEquals(json.readTree("false"), result.at("/result/cancelled"));
        assertFalse(result.has("respCd"));
        assertFalse(result.at("/result").has("extraVipsField"));
        downstream("GET", "/cases/prohibitions/search?prohibitionNoticeNo=" + NOTICE);
        downstream("GET", "/cases/prohibitions/123456789012345");
    }

    @Test
    void missingNoticeReturns404WithoutFetchingDetails() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[]}");
        JsonNode error = responseJson(
                request("GET", "/prohibitions/" + NOTICE, null, AUTH), 404);
        assertEquals("Not Found", error.path("status_message").asString());
        downstream("GET", "/cases/prohibitions/search?prohibitionNoticeNo=" + NOTICE);
    }

    @Test
    void vipsCreateFailureIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":1,\"respMsg\":\"Rejected\"}");
        availableNotice();
        JsonNode error = responseJson(request("POST", "/prohibitions", BODY, AUTH), 500);
        assertEquals("Internal Server Error", error.path("status_message").asString());
        assertFalse(error.has("respMsg"));
        checkedNotice("prohibition", NOTICE);
        downstream("POST", "/cases/prohibitions");
        checkedNotice("prohibition", NOTICE);
    }

    @Test
    void missingVipsProhibitionIdIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":0,\"respMsg\":\"created\"}");
        JsonNode error = responseJson(request("POST", "/prohibitions", BODY, AUTH), 500);
        assertEquals("Internal Server Error", error.path("status_message").asString());
        checkedNotice("prohibition", NOTICE);
        downstream("POST", "/cases/prohibitions");
    }

    @Test
    void vipsCreateWarningCodesAreReportedAsSuccessWithWarning() throws Exception {
        String createPath = "/prohibitions";
        availableNotice();
        vips.enqueue(200, "{\"respCd\":11,\"respMsg\":\"Update Drivers manually\",\"prohibitionId\":123456789012345}");
        JsonNode manualUpdate = responseJson(request("POST", createPath, BODY, AUTH), 201);
        assertEquals("success - Update Drivers manually", manualUpdate.path("respMsg").asString());
        assertEquals(123456789012345L, manualUpdate.path("prohibitionId").asLong());
        checkedNotice("prohibition", NOTICE);
        downstream("POST", "/cases/prohibitions");

        availableNotice();
        vips.enqueue(200, "{\"respCd\":29,\"respMsg\":\"ICBC post execute failed\",\"prohibitionId\":123456789012345}");
        JsonNode icbcFailure = responseJson(request("POST", createPath, BODY, AUTH), 201);
        assertEquals("success - ICBC was not updated", icbcFailure.path("respMsg").asString());
        assertEquals(123456789012345L, icbcFailure.path("prohibitionId").asLong());
        checkedNotice("prohibition", NOTICE);
        downstream("POST", "/cases/prohibitions");
    }

    @Test
    void unexpectedVipsCreateCodeIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":21,\"respMsg\":\"Unexpected\"}");
        JsonNode error = responseJson(request("POST", "/prohibitions", BODY, AUTH), 500);
        assertEquals("Internal Server Error", error.path("status_message").asString());
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

    @Test
    void missingRequiredDocumentIdArrayIsRejectedBeforeCallingVips() throws Exception {
        String body = BODY.replace("\"vipsDocumentIdArray\": [123456789012345],", "");
        int status = request("POST", "/prohibitions", body, AUTH).statusCode();
        assertEquals(400, status, "vipsDocumentIdArray is required by the spec");
        assertTrue(vips.requests.isEmpty(), "Invalid requests must not reach VIPS");
    }
}
