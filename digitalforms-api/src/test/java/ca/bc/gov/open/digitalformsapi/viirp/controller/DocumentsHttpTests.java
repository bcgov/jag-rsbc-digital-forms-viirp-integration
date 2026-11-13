package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import ca.bc.gov.open.digitalformsapi.viirp.utils.UnitTestUtilities;

class DocumentsHttpTests extends HttpApiTestSupport {

    private static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    private static final String NOTICE = "{\"noticeNo\":\"81000029\",\"noticeTypeCd\":\"IRP\"}";

    @Test
    void storesDocumentWith201() throws Exception {
        byte[] pdf = UnitTestUtilities.samplePdf();
        vips.enqueue(200, "{\"status_code\":\"0\",\"status_message\":\"success\",\"document_id\":\"123\"}");
        assertEquals("123", responseJson(upload(pdf), 201)
                .path("document_id").asString());
        var stored = ords("POST", "/ords/storeDocument");
        assertEquals("application/pdf", stored.headers().getFirst("Content-Type"));
        assertArrayEquals(pdf, stored.bodyBytes());
        assertEquals("type_code=MV2702A&mime_type=application&mime_sub_type=pdf&auth_guid=route-test-guid"
                + "&page_count=1", stored.uri().getRawQuery());
    }

    @Test
    void forwardsNoticeMetadataAndPageCountReadFromPdfWithoutAssociating() throws Exception {
        vips.enqueue(201, "{\"status_code\":\"0\",\"document_id\":\"123\"}");
        responseJson(upload(List.of(pdfPart(UnitTestUtilities.samplePdf(3))),
                Map.of("type_code", "MV2723", "notice_type_code", "IRP",
                        "notice_subject_code", "PERS", "pageCount", "abc")), 201);
        var stored = ords("POST", "/ords/storeDocument");
        assertEquals("type_code=MV2723&mime_type=application&mime_sub_type=pdf&auth_guid=route-test-guid"
                + "&notice_type_code=IRP&notice_subject_code=PERS&page_count=3", stored.uri().getRawQuery());
    }

    @Test
    void acceptsVipsMaximumPageCountAndRejectsOnePageMore() throws Exception {
        vips.enqueue(201, "{\"status_code\":\"0\",\"document_id\":\"123\"}");
        responseJson(upload(UnitTestUtilities.samplePdf(99_999)), 201);
        assertTrue(ords("POST", "/ords/storeDocument").uri().getRawQuery().endsWith("&page_count=99999"));
        responseJson(upload(UnitTestUtilities.samplePdf(100_000)), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"file", "anotherFile"})
    void rejectsMultipleFileParts(String secondName) throws Exception {
        byte[] pdf = UnitTestUtilities.samplePdf();
        responseJson(upload(List.of(pdfPart(pdf),
                new UploadPart(secondName, "second.pdf", "application/pdf", pdf)),
                Map.of("type_code", "MV2723")), 400);
    }

    @Test
    void rejectsMissingOrEmptyFile() throws Exception {
        responseJson(upload(List.of(), Map.of("type_code", "MV2723")), 400);
        responseJson(upload(new byte[0]), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not a PDF", "%PDF-1.7\nnot a real document\n%%EOF"})
    void rejectsNonPdfOrMalformedPdfDespitePdfFilenameAndType(String content) throws Exception {
        responseJson(upload(content.getBytes(StandardCharsets.UTF_8)), 400);
    }

    @Test
    void rejectsPasswordProtectedPdfAndPdfWithoutPages() throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            document.save(output);
            responseJson(upload(output.toByteArray()), 400);
            output.reset();
            document.addPage(new PDPage());
            document.protect(new StandardProtectionPolicy("owner-password", "user-password",
                    new AccessPermission()));
            document.save(output);
            responseJson(upload(output.toByteArray()), 400);
        }
    }

    @Test
    void identifiesPdfFromContentRatherThanTrustingFilenameOrMime() throws Exception {
        vips.enqueue(201, "{\"status_code\":\"0\",\"document_id\":\"123\"}");
        responseJson(upload(List.of(new UploadPart("file", "upload.bin", "application/octet-stream",
                UnitTestUtilities.samplePdf())), Map.of("type_code", "MV2723")), 201);
        assertEquals("application/pdf", ords("POST", "/ords/storeDocument").headers().getFirst("Content-Type"));
    }

    @Test
    void rejectsInvalidMultipartMetadata() throws Exception {
        var file = List.of(pdfPart(UnitTestUtilities.samplePdf()));
        for (var fields : List.of(Map.<String, String>of(), Map.of("type_code", " "),
                Map.of("type_code", "x".repeat(11)),
                Map.of("type_code", "MV2723", "notice_type_code", "LONG"),
                Map.of("type_code", "MV2723", "notice_subject_code", "LONGER"))) {
            responseJson(upload(file, fields), 400);
        }
    }

    @Test
    void acceptsExactlyTenMiBAndRejectsOneByteMore() throws Exception {
        byte[] pdf = pdfOfSize(MAX_FILE_BYTES);
        vips.enqueue(201, "{\"status_code\":\"0\",\"document_id\":\"123\"}");
        responseJson(upload(pdf), 201);
        assertArrayEquals(pdf, ords("POST", "/ords/storeDocument").bodyBytes());
        responseJson(upload(pdfOfSize(MAX_FILE_BYTES + 1)), 413);
    }

    @Test
    void rejectsOversizedMultipartRequestAndMalformedMultipart() throws Exception {
        responseJson(upload(List.of(pdfPart(UnitTestUtilities.samplePdf())),
                Map.of("type_code", "x".repeat(11 * 1024 * 1024))), 413);
        responseJson(requestBytes("POST", "/documents", new byte[0], AUTH, "multipart/form-data"), 400);
    }

    @Test
    void rejectsLegacyJsonAndRawBinaryUploads() throws Exception {
        responseJson(request("POST", "/documents", """
                {"type_code":"MV2723","mime_type":"application","mime_sub_type":"pdf","file_object":"YWJj"}
                """, AUTH), 415);
        responseJson(requestBytes("POST", "/documents", UnitTestUtilities.samplePdf(), AUTH,
                "application/pdf"), 415);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "99", "unexpected"})
    void unsuccessfulOrdsStatusIs500(String status) throws Exception {
        vips.enqueue(200, "{\"status_code\":\"" + status + "\",\"status_message\":\"internal detail\"}");
        assertEquals("Internal Server Error",
                responseJson(upload(UnitTestUtilities.samplePdf()), 500)
                        .path("status_message").asString());
        ords("POST", "/ords/storeDocument");
    }

    @Test
    void uploadWithoutReturnedDocumentIdIsNot201() throws Exception {
        vips.enqueue(200, "{\"status_code\":\"0\"}");
        responseJson(upload(UnitTestUtilities.samplePdf()), 500);
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

    private RecordedRequest ords(String method, String path) throws Exception {
        var request = vips.requests.poll(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(method, request.method());
        assertEquals(path, request.uri().getPath());
        assertEquals(basicAuth("ords-test", "ords-test-password"), request.headers().getFirst("Authorization"));
        return request;
    }

    private record UploadPart(String name, String filename, String contentType, byte[] bytes) { }

    private static UploadPart pdfPart(byte[] pdf) {
        return new UploadPart("file", "notice.pdf", "application/pdf", pdf);
    }

    private HttpResponse<String> upload(byte[] pdf) throws Exception {
        return upload(List.of(pdfPart(pdf)), Map.of("type_code", "MV2702A"));
    }

    private HttpResponse<String> upload(List<UploadPart> files, Map<String, String> fields) throws Exception {
        String boundary = "test-" + correlationId;
        List<UploadPart> parts = new ArrayList<>();
        fields.forEach((name, value) -> parts.add(
                new UploadPart(name, null, "text/plain", value.getBytes(StandardCharsets.UTF_8))));
        parts.addAll(files);
        var body = new ByteArrayOutputStream();
        for (var part : parts) {
            String disposition = "Content-Disposition: form-data; name=\"" + part.name() + "\""
                    + (part.filename() == null ? "" : "; filename=\"" + part.filename() + "\"");
            body.write(("--" + boundary + "\r\n" + disposition + "\r\nContent-Type: "
                    + part.contentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(part.bytes());
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return requestBytes("POST", "/documents", body.toByteArray(), AUTH,
                "multipart/form-data; boundary=" + boundary);
    }

    private static byte[] pdfOfSize(int size) throws Exception {
        byte[] pdf = UnitTestUtilities.samplePdf();
        int marker = new String(pdf, StandardCharsets.ISO_8859_1).lastIndexOf("startxref");
        int padding = size - pdf.length;
        byte[] result = new byte[size];
        System.arraycopy(pdf, 0, result, 0, marker);
        Arrays.fill(result, marker, marker + padding, (byte) ' ');
        System.arraycopy(pdf, marker, result, marker + padding, pdf.length - marker);
        return result;
    }
}
