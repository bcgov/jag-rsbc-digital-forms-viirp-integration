package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentsHttpTests extends HttpApiTestSupport {

    private static final String DOCUMENT = """
            {"type_code":"MV2702A","mime_type":"application","mime_sub_type":"pdf","file_object":"YWJj"}
            """;
    private static final String NOTICE = "{\"noticeNo\":\"81000029\",\"noticeTypeCd\":\"IRP\"}";

    @Test
    void storesDocumentWith201() throws Exception {
        vips.enqueue(200, "{\"status_code\":\"0\",\"status_message\":\"success\",\"document_id\":\"123\"}");
        assertEquals("123", responseJson(request("POST", "/documents", DOCUMENT, AUTH), 201)
                .path("document_id").asString());
        ords("POST", "/ords/storeDocument");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "99", "unexpected"})
    void unsuccessfulOrdsStatusIs500(String status) throws Exception {
        vips.enqueue(200, "{\"status_code\":\"" + status + "\",\"status_message\":\"internal detail\"}");
        assertEquals("Internal Server Error",
                responseJson(request("POST", "/documents", DOCUMENT, AUTH), 500)
                        .path("status_message").asString());
        ords("POST", "/ords/storeDocument");
    }

    @Test
    void uploadWithoutReturnedDocumentIdIsNot201() throws Exception {
        vips.enqueue(200, "{\"status_code\":\"0\"}");
        responseJson(request("POST", "/documents", DOCUMENT, AUTH), 500);
        ords("POST", "/ords/storeDocument");
    }

    @Test
    void associatesDocumentWith201AndMissingNoticeIs404() throws Exception {
        vips.enqueue(200, "{\"respCd\":0}");
        responseJson(request("POST", "/documents/association/notice/123", NOTICE, AUTH), 201);
        downstream("POST", "/document-association/notice/123");
        vips.enqueue(200, "{\"respCd\":1,\"respMsg\":\"Notice not found\"}");
        responseJson(request("POST", "/documents/association/notice/123", NOTICE, AUTH), 404);
        downstream("POST", "/document-association/notice/123");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 21, 99})
    void associationFailuresAreNeverSuccess(int code) throws Exception {
        vips.enqueue(200, "{\"respCd\":" + code + "}");
        responseJson(request("POST", "/documents/association/notice/123", NOTICE, AUTH), 500);
        downstream("POST", "/document-association/notice/123");
    }

    @Test
    void retrievesDocumentAndPreservesUpstream404() throws Exception {
        vips.enqueue(200, "YWJj");
        responseJson(request("GET", "/documents/123", null, AUTH), 200);
        downstream("GET", "/documents/123?b64=true&url=false");
        vips.enqueue(404, "{\"message\":\"not found\"}");
        responseJson(request("GET", "/documents/123", null, AUTH), 404);
        downstream("GET", "/documents/123?b64=true&url=false");
        vips.enqueue(200, "");
        responseJson(request("GET", "/documents/123", null, AUTH), 404);
        downstream("GET", "/documents/123?b64=true&url=false");
        vips.enqueue(200, "%%%");
        responseJson(request("GET", "/documents/123", null, AUTH), 500);
        downstream("GET", "/documents/123?b64=true&url=false");
    }

    @ParameterizedTest
    @ValueSource(strings = {"impoundment", "prohibition"})
    void existingNoticeWithNoDocumentsReturnsEmptyCollection(String type) throws Exception {
        if (type.equals("prohibition")) {
            availableNotice();
        }
        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"" + type + "Id\":123}]}");
        vips.enqueue(200, "{\"respCd\":0,\"results\":null}");
        var response = responseJson(request("GET", "/documents/list/81000029", null, AUTH), 200);
        assertEquals(json.readTree("[]"), response.path("results"));
        checkedNotice("impoundment", "81000029");
        if (type.equals("prohibition")) {
            checkedNotice("prohibition", "81000029");
        }
        downstream("GET", "/documents?" + type + "Id=123");
    }

    @Test
    void missingNoticeListIs404ButFailedSearchIs500() throws Exception {
        availableNotice();
        availableNotice();
        responseJson(request("GET", "/documents/list/81000029", null, AUTH), 404);
        checkedNotice("impoundment", "81000029");
        checkedNotice("prohibition", "81000029");

        vips.enqueue(200, "{\"respCd\":21}");
        responseJson(request("GET", "/documents/list/81000029", null, AUTH), 500);
        checkedNotice("impoundment", "81000029");
    }

    @Test
    void populatedDocumentListAndListFailureHaveDistinctResponses() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"impoundmentId\":123}]}");
        vips.enqueue(200, "{\"respCd\":0,\"results\":[{\"documentId\":456,\"documentTypeCd\":\"MV2702A\"}]}");
        var response = responseJson(request("GET", "/documents/list/81000029", null, AUTH), 200);
        assertEquals(456, response.at("/results/0/documentId").asInt());
        checkedNotice("impoundment", "81000029");
        downstream("GET", "/documents?impoundmentId=123");

        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"impoundmentId\":123}]}");
        vips.enqueue(200, "{\"respCd\":21}");
        responseJson(request("GET", "/documents/list/81000029", null, AUTH), 500);
        checkedNotice("impoundment", "81000029");
        downstream("GET", "/documents?impoundmentId=123");
    }

    @Test
    void pingAndCodeTablesHaveDocumentedSuccessAndFailureResponses() throws Exception {
        vips.enqueue(200, "{\"status\":\"UP\"}");
        responseJson(request("GET", "/utility/ords/ping", null, AUTH), 200);
        ords("GET", "/ords/health");
        vips.enqueue(500, "{\"internal\":\"details\"}");
        responseJson(request("GET", "/utility/ords/ping", null, AUTH), 500);
        ords("GET", "/ords/health");

        vips.enqueue(200, "{\"configuration\":{},\"respMsg\":\"Success\"}");
        responseJson(request("GET", "/codetables", null, AUTH), 200);
        var codeTablesRequest = vips.requests.poll(2, TimeUnit.SECONDS);
        assertNotNull(codeTablesRequest);
        assertEquals("/configuration", codeTablesRequest.uri().getPath());
        vips.enqueue(200, "{\"respCd\":1,\"respMsg\":\"Failure\"}");
        responseJson(request("GET", "/codetables", null, AUTH), 500);
        assertNotNull(vips.requests.poll(2, TimeUnit.SECONDS));
    }

    private void ords(String method, String path) throws Exception {
        var request = vips.requests.poll(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(method, request.method());
        assertEquals(path, request.uri().getPath());
        assertEquals(basicAuth("ords-test", "ords-test-password"), request.headers().getFirst("Authorization"));
    }
}
