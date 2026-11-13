package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class ImpoundmentsHttpTests extends HttpApiTestSupport {

    private static final String NOTICE = "00123456";
    static final String BODY = """
            {
              "vipsDocumentIdArray": [123456789012345], "vipsImpoundmentArray": [],
              "vipsImpoundCreate": {
                "impoundmentNoticeNo": "00123456", "dlJurisdictionCd": "BC",
                "impoundmentDt": "2022-10-11T00:00:00.000-07:00",
                "noticeSubjectCd": "VEHI", "noticeTypeCd": "IMP",
                "originalCauseCds": ["STUNT"], "policeDetatchmentId": 304, "policeFileNo": "HTTP-TEST"
              },
              "vipsRegistrationCreateArray": [{
                "dataSourceCd": "VIPS", "registrationRoleCd": "REGOWN",
                "surnameNm": "HTTP TEST", "vipsAddressArray": [],
                "vipsLicenceObj": {"driverLicenceNo": "00001234", "dlJurisdictionCd": "BC"}
              }],
              "vipsVehicleCreate": {"registrationNo": "00005678", "licencePlateNo": "TEST"}
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
        assertEquals(json.readTree("{\"respMsg\":\"success\",\"impoundmentId\":123456789012345}"),
                responseJson(request("POST", createPath, BODY, AUTH), 201));
        checkedNotice("impoundment", NOTICE);
        JsonNode submitted = json.readTree(downstream("POST", "/cases/impoundments").body());
        assertEquals(NOTICE, submitted.at("/vipsImpoundCreate/impoundmentNoticeNo").asString());
        assertEquals(123456789012345L, submitted.at("/vipsDocumentIdArray/0").asLong());
        assertEquals(json.readTree("[\"STUNT\"]"), submitted.at("/vipsImpoundCreate/originalCauseCds"));
        assertEquals("00001234", submitted.at("/vipsRegistrationCreateArray/0/vipsLicenceObj/driverLicenceNo").asString());
        assertFalse(submitted.at("/vipsRegistrationCreateArray/0").has("vipsLicenceCreateObj"),
                "VIPS WS only binds vipsLicenceObj; any other licence property name is silently dropped");
        assertEquals("00005678", submitted.at("/vipsVehicleCreate/registrationNo").asString());

        vips.enqueue(200, "{\"respCd\":0,\"resultsCount\":null,\"result\":[{\"impoundmentId\":123456789012345}]}");
        vips.enqueue(200, """
                {"respCd":0,"result":{"impoundmentNoticeNo":"%s","dlJurisdictionCd":"BC",
                 "impoundmentDt":"%s","extraVipsField":"ignored"}}
                """.formatted(NOTICE, submitted.at("/vipsImpoundCreate/impoundmentDt").asString()));
        JsonNode result = responseJson(request("GET", getPath, null, AUTH), 200);
        assertEquals(NOTICE, result.at("/result/impoundmentNoticeNo").asString());
        assertEquals("BC", result.at("/result/dlJurisdictionCd").asString());
        assertEquals("2022-10-11T00:00:00.000-07:00", result.at("/result/impoundmentDt").asString());
        assertFalse(result.has("respCd"));
        assertFalse(result.at("/result").has("extraVipsField"));
        downstream("GET", "/cases/impoundments/search?impoundmentNoticeNo=" + NOTICE);
        downstream("GET", "/cases/impoundments/123456789012345");
    }

    @Test
    void missingNoticeReturns404WithoutFetchingDetails() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[]}");
        JsonNode error = responseJson(
                request("GET", "/impoundments/" + NOTICE, null, AUTH), 404);
        assertEquals("Not Found", error.path("status_message").asString());
        downstream("GET", "/cases/impoundments/search?impoundmentNoticeNo=" + NOTICE);
    }

    @Test
    void vipsCreateFailureIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":99,\"respMsg\":\"Rejected\"}");
        JsonNode error = responseJson(request("POST", "/impoundments", BODY, AUTH), 500);
        assertEquals("Internal Server Error", error.path("status_message").asString());
        assertFalse(error.has("respMsg"));
        checkedNotice("impoundment", NOTICE);
        downstream("POST", "/cases/impoundments");
    }

    @Test
    void missingVipsImpoundmentIdIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":0,\"respMsg\":\"created\"}");
        JsonNode error = responseJson(request("POST", "/impoundments", BODY, AUTH), 500);
        assertEquals("Internal Server Error", error.path("status_message").asString());
        checkedNotice("impoundment", NOTICE);
        downstream("POST", "/cases/impoundments");
    }

    @Test
    void vipsCreateWarningCodesAreReportedAsSuccessWithWarning() throws Exception {
        String createPath = "/impoundments";
        availableNotice();
        vips.enqueue(200, "{\"respCd\":29,\"respMsg\":\"ICBC post execute failed\",\"impoundmentId\":123456789012345}");
        JsonNode icbcFailure = responseJson(request("POST", createPath, BODY, AUTH), 201);
        assertEquals("success - ICBC was not updated", icbcFailure.path("respMsg").asString());
        assertEquals(123456789012345L, icbcFailure.path("impoundmentId").asLong());
        checkedNotice("impoundment", NOTICE);
        downstream("POST", "/cases/impoundments");

        availableNotice();
        vips.enqueue(200, "{\"respCd\":11,\"respMsg\":\"Update Drivers manually\",\"impoundmentId\":123456789012345}");
        JsonNode manualUpdate = responseJson(request("POST", createPath, BODY, AUTH), 201);
        assertEquals("success - Update Drivers manually", manualUpdate.path("respMsg").asString());
        assertEquals(123456789012345L, manualUpdate.path("impoundmentId").asLong());
        checkedNotice("impoundment", NOTICE);
        downstream("POST", "/cases/impoundments");
    }

    @Test
    void unexpectedVipsCreateCodeIsNotReportedAsSuccess() throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":21,\"respMsg\":\"Unexpected\"}");
        JsonNode error = responseJson(request("POST", "/impoundments", BODY, AUTH), 500);
        assertEquals("Internal Server Error", error.path("status_message").asString());
        checkedNotice("impoundment", NOTICE);
        downstream("POST", "/cases/impoundments");
    }

    @Test
    void missingDriverLicenceIsRejectedBeforeCallingVips() throws Exception {
        JsonNode error = responseJson(request("POST", "/impoundments",
                BODY.replace("\"00001234\"", "\"\""), AUTH), 400);
        assertEquals("Invalid license object - DL is a required field.", error.path("status_message").asString());
    }
}
