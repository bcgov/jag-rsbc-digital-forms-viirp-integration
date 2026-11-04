package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class ImpoundmentsHttpTests extends HttpApiTestSupport {

    private static final String NOTICE = "00123456";
    private static final String BODY = """
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
                "vipsLicenceCreateObj": {"driverLicenceNo": "00001234", "dlJurisdictionCd": "BC"}
              }],
              "vipsVehicleCreate": {"registrationNo": "00005678", "licencePlateNo": "TEST"}
            }
            """;

    @Test
    void authenticatesCreatesAndReadsThroughVipsHttp() throws Exception {
        String createPath = "/impoundments/" + correlationId;
        String getPath = "/impoundments/" + NOTICE + "/" + correlationId;
        responseJson(request("POST", createPath, BODY, null), 401);
        responseJson(request("GET", getPath, null, basicAuth("route-test", "wrong")), 401);
        assertTrue(vips.requests.isEmpty(), "Unauthorized requests must not reach VIPS");

        vips.enqueue(200, "{\"respCd\":0,\"respMsg\":\"created\",\"extraVipsMetadata\":\"ignored\"}");
        assertEquals(json.readTree("{\"respMsg\":\"success\"}"),
                responseJson(request("POST", createPath, BODY, AUTH), 200));
        JsonNode submitted = json.readTree(downstream("POST", "/cases/impoundments").body());
        assertEquals(NOTICE, submitted.at("/vipsImpoundCreate/impoundmentNoticeNo").asText());
        assertEquals(123456789012345L, submitted.at("/vipsDocumentIdArray/0").asLong());
        assertEquals(json.readTree("[\"STUNT\"]"), submitted.at("/vipsImpoundCreate/originalCauseCds"));
        assertEquals("00001234", submitted.at("/vipsRegistrationCreateArray/0/vipsLicenceCreateObj/driverLicenceNo").asText());
        assertEquals("00005678", submitted.at("/vipsVehicleCreate/registrationNo").asText());

        vips.enqueue(200, "{\"respCd\":0,\"resultsCount\":null,\"result\":[{\"impoundmentId\":123456789012345}]}");
        vips.enqueue(200, """
                {"respCd":0,"result":{"impoundmentNoticeNo":"%s","dlJurisdictionCd":"BC",
                 "impoundmentDt":"%s","extraVipsField":"ignored"}}
                """.formatted(NOTICE, submitted.at("/vipsImpoundCreate/impoundmentDt").asText()));
        JsonNode result = responseJson(request("GET", getPath, null, AUTH), 200);
        assertEquals(NOTICE, result.at("/result/impoundmentNoticeNo").asText());
        assertEquals("BC", result.at("/result/dlJurisdictionCd").asText());
        assertEquals("2022-10-11T00:00:00.000-07:00", result.at("/result/impoundmentDt").asText());
        assertFalse(result.has("respCd"));
        assertFalse(result.at("/result").has("extraVipsField"));
        downstream("GET", "/cases/impoundments/search?impoundmentNoticeNo=" + NOTICE);
        downstream("GET", "/cases/impoundments/123456789012345");
    }

    @Test
    void missingNoticeReturns404WithoutFetchingDetails() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[]}");
        JsonNode error = responseJson(
                request("GET", "/impoundments/" + NOTICE + "/" + correlationId, null, AUTH), 404);
        assertEquals("Not Found", error.path("status_message").asText());
        downstream("GET", "/cases/impoundments/search?impoundmentNoticeNo=" + NOTICE);
    }

    @Test
    void vipsCreateFailureIsNotReportedAsSuccess() throws Exception {
        vips.enqueue(200, "{\"respCd\":99,\"respMsg\":\"Rejected\"}");
        JsonNode error = responseJson(request("POST", "/impoundments/" + correlationId, BODY, AUTH), 500);
        assertTrue(error.path("status_message").asText().contains(NOTICE));
        assertFalse(error.has("respMsg"));
        downstream("POST", "/cases/impoundments");
    }

    @Test
    void missingDriverLicenceIsRejectedBeforeCallingVips() throws Exception {
        JsonNode error = responseJson(request("POST", "/impoundments/" + correlationId,
                BODY.replace("\"00001234\"", "\"\""), AUTH), 400);
        assertEquals("Invalid license object - DL is a required field.", error.path("status_message").asText());
    }
}
