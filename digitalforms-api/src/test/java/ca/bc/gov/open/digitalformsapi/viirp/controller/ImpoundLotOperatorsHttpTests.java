package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ImpoundLotOperatorsHttpTests extends HttpApiTestSupport {

    @Test
    void returnsStructuredDetailsFromOneOptInConfigurationRequest() throws Exception {
        vips.enqueue(200, """
                {"respCd":0,"configuration":{
                 "impoundLotOperators":[{"identityId":1,"identityNm":"old label","activeYN":"Y"}],
                 "impoundLotOperatorDetails":[
                  {"identityId":1,"legalNm":"Example Lot","businessNm":"Example Business",
                   "addressFirstLineTxt":"123 Main St","addressSecondLineTxt":null,
                   "addressThirdLineTxt":"Unit 2","cityNm":"Victoria","provinceCd":"BC","postalCodeTxt":"V1V1V1",
                   "phoneNumbers":["2505550101","2505550102"],"faxNumbers":["2505550104","2505550105"]},
                  {"identityId":2,"phoneNumbers":[],"faxNumbers":[]}]}}
                """);

        var response = responseJson(request("GET", "/codetables", null, AUTH), 200);
        var operators = response.at("/configuration/impoundLotOperators");
        assertEquals(2, operators.size());
        var first = operators.get(0);
        assertEquals(1, first.path("identityId").asInt());
        assertEquals("Example Lot", first.path("legalNm").asString());
        assertEquals("Example Business", first.path("businessNm").asString());
        assertEquals("123 Main St", first.path("addressFirstLineTxt").asString());
        assertTrue(first.path("addressSecondLineTxt").isNull());
        assertEquals("Unit 2", first.path("addressThirdLineTxt").asString());
        assertEquals("Victoria", first.path("cityNm").asString());
        assertEquals("BC", first.path("provinceCd").asString());
        assertEquals("V1V1V1", first.path("postalCodeTxt").asString());
        assertEquals(json.readTree("[\"2505550101\",\"2505550102\"]"), first.path("phoneNumbers"));
        assertEquals(json.readTree("[\"2505550104\",\"2505550105\"]"), first.path("faxNumbers"));
        assertFalse(first.has("identityNm"));
        assertTrue(operators.get(1).path("legalNm").isNull());
        assertEquals(0, operators.get(1).path("phoneNumbers").size());
        assertEquals(0, operators.get(1).path("faxNumbers").size());
        downstream("GET", "/configuration?includeImpoundLotOperatorDetails=true");
    }

    @Test
    void retrievesLargeConfiguration() throws Exception {
        var operators = json.createArrayNode();
        for (int id = 1; id <= 1200; id++) {
            var operator = operators.addObject();
            operator.put("identityId", id);
            operator.put("legalNm", "Synthetic operator name " + id);
            operator.put("addressFirstLineTxt", "Synthetic address line one");
            operator.put("addressSecondLineTxt", "Synthetic address line two");
            operator.put("addressThirdLineTxt", "Synthetic address line three");
            operator.put("cityNm", "Synthetic City");
            operator.putArray("phoneNumbers").add("2505550101");
            operator.putArray("faxNumbers");
        }
        String payload = "{\"respCd\":0,\"configuration\":{\"impoundLotOperatorDetails\":" + operators + "}}";
        assertTrue(payload.length() > 256 * 1024);
        vips.enqueue(200, payload);
        var response = responseJson(request("GET", "/codetables", null, AUTH), 200);
        assertEquals(1200, response.at("/configuration/impoundLotOperators").size());
        downstream("GET", "/configuration?includeImpoundLotOperatorDetails=true");
    }

    @Test
    void oversizedConfigurationFailsWithoutPartialResults() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"configuration\":{\"impoundLotOperatorDetails\":[],\"padding\":\""
                + "x".repeat(8 * 1024 * 1024) + "\"}}");
        responseJson(request("GET", "/codetables", null, AUTH), 502);
        downstream("GET", "/configuration?includeImpoundLotOperatorDetails=true");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"respCd\":1,\"respMsg\":\"sensitive\"}",
            "{\"respCd\":0,\"configuration\":{}}",
            "{\"respCd\":0,\"configuration\":{\"impoundLotOperatorDetails\":null}}",
            "{\"respCd\":0,\"configuration\":{\"impoundLotOperatorDetails\":[{\"identityId\":1}]}}",
            "{\"respCd\":0,\"configuration\":{\"impoundLotOperatorDetails\":[{\"identityId\":1,\"phoneNumbers\":[null],\"faxNumbers\":[]}]}}",
            "{\"respCd\":0,\"configuration\":{\"impoundLotOperatorDetails\":[{\"identityId\":1,\"phoneNumbers\":[],\"faxNumbers\":[]},{\"identityId\":1,\"phoneNumbers\":[],\"faxNumbers\":[]}]}}"
    })
    void invalidConfigurationFailsWithoutPartialResults(String body) throws Exception {
        vips.enqueue(200, body);
        var error = responseJson(request("GET", "/codetables", null, AUTH), 502);
        assertEquals(Set.of("type", "title", "status", "detail", "instance"), error.propertyNames());
        assertEquals("Invalid response from VIPS.", error.path("detail").asString());
        downstream("GET", "/configuration?includeImpoundLotOperatorDetails=true");
    }
}
