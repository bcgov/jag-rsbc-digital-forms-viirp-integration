package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class ProhibitionsHttpTests extends HttpApiTestSupport {

    private static final String NOTICE = "21900601";
    private static final String BODY = """
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
        String createPath = "/prohibitions/" + correlationId;
        String getPath = "/prohibitions/" + NOTICE + "/" + correlationId;
        responseJson(request("POST", createPath, BODY, null), 401);
        responseJson(request("GET", getPath, null, basicAuth("route-test", "wrong")), 401);
        assertTrue(vips.requests.isEmpty(), "Unauthorized requests must not reach VIPS");

        vips.enqueue(200, """
                {"respCd":0,"respMsg":"created","extraVipsMetadata":"ignored"}
                """);
        assertEquals(json.readTree("{\"respMsg\":\"success\"}"),
                responseJson(request("POST", createPath, BODY, AUTH), 200));
        JsonNode submitted = json.readTree(downstream("POST", "/cases/prohibitions").body());
        assertEquals(NOTICE, submitted.at("/vipsProhibitionCreate/prohibitionNoticeNo").asText());
        assertEquals(123456789012345L, submitted.at("/vipsDocumentIdArray/0").asLong());
        assertEquals("N", submitted.at("/vipsProhibitionCreate/driverLicenceSeizedYN").asText());
        assertEquals("00001234", submitted.at("/vipsRegistrationCreate/vipsLicenceObj/driverLicenceNo").asText());
        assertFalse(submitted.at("/vipsRegistrationCreate").has("vipsLicenceCreateObj"),
                "VIPS WS only binds vipsLicenceObj; any other licence property name is silently dropped");
        assertTrue(submitted.at("/vipsProhibitionCreate/expiryDt").isNull());

        vips.enqueue(200, """
                {"respCd":0,"resultsCount":null,"result":[{"prohibitionId":123456789012345}]}
                """);
        vips.enqueue(200, """
                {"respCd":0,"result":{"prohibitionNoticeNo":"%s","cityNm":"%s",
                 "effectiveDt":"%s","cancelled":false,"extraVipsField":"ignored"}}
                """.formatted(NOTICE, submitted.at("/vipsProhibitionCreate/cityNm").asText(),
                        submitted.at("/vipsProhibitionCreate/effectiveDt").asText()));
        JsonNode result = responseJson(request("GET", getPath, null, AUTH), 200);
        assertEquals(NOTICE, result.at("/result/prohibitionNoticeNo").asText());
        assertEquals("VICTORIA", result.at("/result/cityNm").asText());
        assertEquals("2022-10-11T00:00:00.000-07:00", result.at("/result/effectiveDt").asText());
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
                request("GET", "/prohibitions/" + NOTICE + "/" + correlationId, null, AUTH), 404);
        assertEquals("Not Found", error.path("status_message").asText());
        downstream("GET", "/cases/prohibitions/search?prohibitionNoticeNo=" + NOTICE);
    }

    @Test
    void vipsCreateFailureIsNotReportedAsSuccess() throws Exception {
        vips.enqueue(200, "{\"respCd\":1,\"respMsg\":\"Rejected\"}");
        JsonNode error = responseJson(request("POST", "/prohibitions/" + correlationId, BODY, AUTH), 500);
        assertTrue(error.path("status_message").asText().contains(NOTICE));
        assertFalse(error.has("respMsg"));
        downstream("POST", "/cases/prohibitions");
    }

    @Test
    void vipsCreateWarningCodesAreReportedAsSuccessWithWarning() throws Exception {
        String createPath = "/prohibitions/" + correlationId;
        vips.enqueue(200, "{\"respCd\":11,\"respMsg\":\"Update Drivers manually\"}");
        assertEquals("success - Update Drivers manually",
                responseJson(request("POST", createPath, BODY, AUTH), 200).path("respMsg").asText());
        downstream("POST", "/cases/prohibitions");

        vips.enqueue(200, "{\"respCd\":29,\"respMsg\":\"ICBC post execute failed\"}");
        assertEquals("success - ICBC was not updated",
                responseJson(request("POST", createPath, BODY, AUTH), 200).path("respMsg").asText());
        downstream("POST", "/cases/prohibitions");
    }

    @Test
    void unexpectedVipsCreateCodeIsNotReportedAsSuccess() throws Exception {
        vips.enqueue(200, "{\"respCd\":21,\"respMsg\":\"Unexpected\"}");
        JsonNode error = responseJson(request("POST", "/prohibitions/" + correlationId, BODY, AUTH), 500);
        assertTrue(error.path("status_message").asText().contains("21"));
        assertTrue(error.path("status_message").asText().contains(NOTICE));
        downstream("POST", "/cases/prohibitions");
    }

    @Test
    void prohibitionCityLongerThanVipsColumnIsRejectedBeforeCallingVips() throws Exception {
        String tooLongCity = "\"" + "X".repeat(31) + "\"";
        int status = request("POST", "/prohibitions/" + correlationId,
                BODY.replace("\"VICTORIA\"", tooLongCity), AUTH).statusCode();
        assertTrue(status >= 400, "31-character cityNm must be rejected; VIPS city_nm is VARCHAR2(30)");
        assertTrue(vips.requests.isEmpty(), "Invalid requests must not reach VIPS");
    }
}
