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
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import ca.bc.gov.open.digitalformsapi.viirp.utils.UnitTestUtilities;
import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiClient;

class DocumentsHttpTests extends HttpApiTestSupport {

    private static final int MAX_FILE_BYTES = 10 * 1024 * 1024;

    @Autowired
    private ApiClient ordsClient;

    @Autowired
    private ConfigProperties properties;

    @ParameterizedTest
    @ValueSource(strings = {"1", "123", "9223372036854775807"})
    void storesDocumentWith201AndReadsTheLocation(String documentId) throws Exception {
        byte[] pdf = UnitTestUtilities.samplePdf();
        queueNotice("prohibition", "IRP");
        vips.enqueue(200, "{\"status_code\":\"0\",\"status_message\":\"success\",\"document_id\":\"" + documentId + "\"}");
        vips.enqueue(200, "{\"respCd\":0}");
        var created = upload(pdf);
        assertEquals(json.readTree("{\"documentId\":" + documentId + "}"), responseJson(created, 201));
        String resourcePath = createdResourcePath(created, "/documents/" + documentId);
        checkUploadNotice("prohibition");
        var stored = ords("POST", "/ords/storeDocument");
        assertEquals("application/pdf", stored.headers().getFirst("Content-Type"));
        assertArrayEquals(pdf, stored.bodyBytes());
        assertEquals("type_code=MV2702A&mime_type=application&mime_sub_type=pdf&auth_guid=route-test-guid"
                + "&notice_type_code=IRP&notice_subject_code=PERS&page_count=1", stored.uri().getRawQuery());
        checkAssociation(documentId, "IRP");

        responseJson(request("GET", resourcePath, null, null, null, "application/pdf"), 401);
        vips.enqueue(200, stored.bodyBytes(), "application/x-download");
        var retrieved = requestBinary("GET", resourcePath, AUTH, "application/pdf");
        assertEquals(200, retrieved.statusCode());
        assertArrayEquals(pdf, retrieved.body());
        downstream("GET", "/documents/" + documentId + "?b64=false&url=false");
    }

    @ParameterizedTest
    @CsvSource({"prohibition,IRP,PERS", "prohibition,ADP,PERS", "prohibition,UL,PERS", "impoundment,IMP,VEHI"})
    void uploadsAndAssociatesUsingServerOwnedNoticeMetadata(String kind, String type, String subject) throws Exception {
        byte[] pdf = UnitTestUtilities.samplePdf(3);
        var files = List.of(pdfPart(pdf));
        var fields = Map.of("documentTypeCd", "MV2723", "noticeTypeCd", "IGNORED",
                "noticeSubjectCd", "IGNORED", "pageCount", "999");
        responseJson(upload(kind, files, fields, null), 401);
        queueNotice(kind, type);
        vips.enqueue(201, "{\"status_code\":\"0\",\"document_id\":\"123\"}");
        vips.enqueue(200, "{\"respCd\":0}");
        var response = upload(kind, files, fields, AUTH);
        assertEquals(json.readTree("{\"documentId\":123}"), responseJson(response, 201));
        createdResourcePath(response, "/documents/123");
        checkUploadNotice(kind);
        var stored = ords("POST", "/ords/storeDocument");
        assertArrayEquals(pdf, stored.bodyBytes());
        assertEquals("type_code=MV2723&mime_type=application&mime_sub_type=pdf&auth_guid=route-test-guid"
                + "&notice_type_code=" + type + "&notice_subject_code=" + subject + "&page_count=3",
                stored.uri().getRawQuery());
        checkAssociation("123", type);
        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"" + kind + "Id\":321}]}");
        vips.enqueue(200, "{\"respCd\":0,\"results\":[{\"documentId\":123,\"documentTypeCd\":\"MV2723\"}]}");
        assertEquals(123, responseJson(request("GET", documentListPath(kind), null, AUTH), 200)
                .at("/0/documentId").asInt());
        checkedNotice(kind, "81000029");
        downstream("GET", "/documents?" + kind + "Id=321");
    }

    @Test
    void acceptsVipsMaximumPageCountAndRejectsOnePageMore() throws Exception {
        queueNotice("prohibition", "IRP");
        vips.enqueue(201, "{\"status_code\":\"0\",\"document_id\":\"123\"}");
        vips.enqueue(200, "{\"respCd\":0}");
        responseJson(upload(UnitTestUtilities.samplePdf(99_999)), 201);
        checkUploadNotice("prohibition");
        assertTrue(ords("POST", "/ords/storeDocument").uri().getRawQuery().endsWith("&page_count=99999"));
        checkAssociation("123", "IRP");
        responseJson(upload(UnitTestUtilities.samplePdf(100_000)), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"file", "anotherFile"})
    void rejectsMultipleFileParts(String secondName) throws Exception {
        byte[] pdf = UnitTestUtilities.samplePdf();
        responseJson(upload(List.of(pdfPart(pdf),
                new UploadPart(secondName, "second.pdf", "application/pdf", pdf)),
                Map.of("documentTypeCd", "MV2723")), 400);
    }

    @Test
    void rejectsMissingOrEmptyFile() throws Exception {
        responseJson(upload(List.of(), Map.of("documentTypeCd", "MV2723")), 400);
        responseJson(upload(new byte[0]), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not a PDF", "%PDF-1.7\nnot a real document\n%%EOF"})
    void rejectsNonPdfOrMalformedPdfDespitePdfFilenameAndType(String content) throws Exception {
        var problem = responseJson(upload(content.getBytes(StandardCharsets.UTF_8)), 400);
        assertEquals("file", problem.at("/errors/0/field").asString());
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
        queueNotice("prohibition", "IRP");
        vips.enqueue(201, "{\"status_code\":\"0\",\"document_id\":\"123\"}");
        vips.enqueue(200, "{\"respCd\":0}");
        responseJson(upload(List.of(new UploadPart("file", "upload.bin", "application/octet-stream",
                UnitTestUtilities.samplePdf())), Map.of("documentTypeCd", "MV2723")), 201);
        checkUploadNotice("prohibition");
        assertEquals("application/pdf", ords("POST", "/ords/storeDocument").headers().getFirst("Content-Type"));
        checkAssociation("123", "IRP");
    }

    @Test
    void rejectsInvalidMultipartMetadata() throws Exception {
        var file = List.of(pdfPart(UnitTestUtilities.samplePdf()));
        for (var invalid : List.of(Map.entry("documentTypeCd", Map.<String, String>of()),
                Map.entry("documentTypeCd", Map.of("type_code", "MV2723")),
                Map.entry("documentTypeCd", Map.of("documentTypeCd", " ")),
                Map.entry("documentTypeCd", Map.of("documentTypeCd", "x".repeat(11))))) {
            var problem = responseJson(upload(file, invalid.getValue()), 400);
            assertEquals(invalid.getKey(), problem.at("/errors/0/field").asString(), problem.toString());
        }
    }

    @Test
    void acceptsExactlyTenMiBAndRejectsOneByteMore() throws Exception {
        byte[] pdf = pdfOfSize(MAX_FILE_BYTES);
        queueNotice("prohibition", "IRP");
        vips.enqueue(201, "{\"status_code\":\"0\",\"document_id\":\"123\"}");
        vips.enqueue(200, "{\"respCd\":0}");
        responseJson(upload(pdf), 201);
        checkUploadNotice("prohibition");
        assertArrayEquals(pdf, ords("POST", "/ords/storeDocument").bodyBytes());
        checkAssociation("123", "IRP");
        responseJson(upload(pdfOfSize(MAX_FILE_BYTES + 1)), 413);
    }

    @Test
    void rejectsOversizedMultipartRequestAndMalformedMultipart() throws Exception {
        responseJson(upload(List.of(pdfPart(UnitTestUtilities.samplePdf())),
                Map.of("documentTypeCd", "x".repeat(11 * 1024 * 1024))), 413);
        responseJson(requestBytes("POST", documentListPath("prohibition"), new byte[0], AUTH, "multipart/form-data"), 400);
    }

    @Test
    void rejectsLegacyJsonAndRawBinaryUploads() throws Exception {
        responseJson(request("POST", documentListPath("prohibition"), """
                {"type_code":"MV2723","mime_type":"application","mime_sub_type":"pdf","file_object":"YWJj"}
                """, AUTH), 415);
        responseJson(requestBytes("POST", documentListPath("prohibition"), UnitTestUtilities.samplePdf(), AUTH,
                "application/pdf"), 415);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "99", "unexpected"})
    void unsuccessfulOrdsStatusIs502(String status) throws Exception {
        queueNotice("prohibition", "IRP");
        vips.enqueue(200, "{\"status_code\":\"" + status + "\",\"status_message\":\"internal detail\"}");
        assertEquals("Invalid response from VIPS.",
                responseJson(upload(UnitTestUtilities.samplePdf()), 502)
                        .path("detail").asString());
        checkUploadNotice("prohibition");
        ords("POST", "/ords/storeDocument");
    }

    @Test
    void uploadWithoutReturnedDocumentIdIsNot201() throws Exception {
        queueNotice("prohibition", "IRP");
        vips.enqueue(200, "{\"status_code\":\"0\"}");
        responseJson(upload(UnitTestUtilities.samplePdf()), 502);
        checkUploadNotice("prohibition");
        ords("POST", "/ords/storeDocument");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-an-id", "1/2", "9223372036854775808"})
    void uploadWithUnusableDocumentIdDoesNotReturnACreatedLocation(String documentId) throws Exception {
        queueNotice("prohibition", "IRP");
        vips.enqueue(200, "{\"status_code\":\"0\",\"document_id\":\"" + documentId + "\"}");
        var response = upload(UnitTestUtilities.samplePdf());
        responseJson(response, 502);
        assertTrue(response.headers().allValues("Location").isEmpty());
        checkUploadNotice("prohibition");
        ords("POST", "/ords/storeDocument");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{", "[]", "{}", "{\"status_code\":{}}"})
    void malformedOrdsUploadResponseIs502(String body) throws Exception {
        queueNotice("prohibition", "IRP");
        vips.enqueue(200, body);
        responseJson(upload(UnitTestUtilities.samplePdf()), 502);
        checkUploadNotice("prohibition");
        ords("POST", "/ords/storeDocument");
    }

    @ParameterizedTest
    @CsvSource({"500,502", "503,503", "504,504"})
    void ordsUploadHttpFailureUsesTheUpstreamStatusContract(int upstreamStatus, int expectedStatus) throws Exception {
        queueNotice("prohibition", "IRP");
        vips.enqueue(upstreamStatus, "{\"error\":\"sensitive upstream information\"}");
        var response = upload(UnitTestUtilities.samplePdf());
        responseJson(response, expectedStatus);
        assertFalse(response.body().contains("sensitive upstream information"));
        assertEquals(expectedStatus == 503 ? List.of("30") : List.of(),
                response.headers().allValues("Retry-After"));
        checkUploadNotice("prohibition");
        ords("POST", "/ords/storeDocument");
    }

    @Test
    void ordsReadTimeoutIs504WithoutResendingTheDocument() throws Exception {
        int readTimeout = ordsClient.getReadTimeout();
        try {
            ordsClient.setReadTimeout(200);
            queueNotice("prohibition", "IRP");
            vips.responses.add(new StubResponse(200,
                    "{\"status_code\":\"0\",\"document_id\":\"123\"}".getBytes(StandardCharsets.UTF_8),
                    "application/json", Map.of(), 1000, 0));
            responseJson(upload(UnitTestUtilities.samplePdf()), 504);
            checkUploadNotice("prohibition");
            ords("POST", "/ords/storeDocument");
        } finally {
            ordsClient.setReadTimeout(readTimeout);
        }
    }

    @Test
    void missingLocalOrdsAuditGuidRemains500WithoutUploading() throws Exception {
        String authGuid = properties.getVipsRestApiCredentialsGuid();
        try {
            properties.setVipsRestApiCredentialsGuid(null);
            queueNotice("prohibition", "IRP");
            var response = upload(UnitTestUtilities.samplePdf());
            assertEquals("Internal Server Error", responseJson(response, 500).path("detail").asString());
            for (String path : List.of("/cases/prohibitions/search?prohibitionNoticeNo=81000029",
                    "/cases/prohibitions/321")) {
                var lookup = vips.requests.poll(2, TimeUnit.SECONDS);
                assertNotNull(lookup);
                assertEquals("GET", lookup.method());
                assertEquals(path, lookup.uri().toString());
                assertNull(lookup.headers().getFirst("smgov_userguid"));
            }
            assertTrue(response.headers().allValues("Retry-After").isEmpty());
        } finally {
            properties.setVipsRestApiCredentialsGuid(authGuid);
        }
    }

    @Test
    void nonPdfUpstreamBodyIs502BeforeTheDownloadCommits() throws Exception {
        vips.enqueue(200, "<html>upstream error details</html>".getBytes(StandardCharsets.UTF_8), "text/html");
        var response = request("GET", "/documents/123", null, AUTH, null, "application/pdf");
        responseJson(response, 502);
        assertFalse(response.body().contains("upstream error details"));
        assertTrue(response.headers().allValues("Content-Disposition").isEmpty());
        downstream("GET", "/documents/123?b64=false&url=false");
    }

    @Test
    void stalledPdfBodyIs504BeforeTheDownloadCommits() throws Exception {
        vips.responses.add(new StubResponse(200, UnitTestUtilities.samplePdf(),
                "application/x-download", Map.of(), 0, 3000));
        var response = request("GET", "/documents/123", null, AUTH, null, "application/pdf");
        responseJson(response, 504);
        assertTrue(response.headers().allValues("Content-Disposition").isEmpty());
        downstream("GET", "/documents/123?b64=false&url=false");
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "application/problem+json, application/pdf;q=0.5"})
    void streamsPdfWithDownloadHeadersWithoutBase64Encoding(String accept) throws Exception {
        byte[] pdf = UnitTestUtilities.samplePdf();
        vips.enqueue(200, pdf, "application/x-download");
        var response = requestBinary("GET", "/documents/123", AUTH, accept);
        assertEquals(200, response.statusCode());
        assertArrayEquals(pdf, response.body());
        assertEquals("application/pdf", response.headers().firstValue("Content-Type").orElseThrow());
        assertEquals("attachment; filename=\"document-123.pdf\"",
                response.headers().firstValue("Content-Disposition").orElseThrow());
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals(pdf.length, Long.parseLong(response.headers().firstValue("Content-Length").orElseThrow()));
        downstream("GET", "/documents/123?b64=false&url=false");
    }

    @Test
    void missingDocumentIs404ForHttp404AndEmptyVipsResponses() throws Exception {
        vips.enqueue(404, "{\"message\":\"not found\"}");
        responseJson(request("GET", "/documents/123", null, AUTH, null, "application/pdf"), 404);
        downstream("GET", "/documents/123?b64=false&url=false");
        vips.enqueue(204, new byte[0], "application/x-download");
        responseJson(request("GET", "/documents/123", null, AUTH, null, "application/pdf"), 404);
        downstream("GET", "/documents/123?b64=false&url=false");
    }

    @ParameterizedTest
    @ValueSource(strings = {"impoundment", "prohibition"})
    void typedDocumentListsReturnEmptyCollectionsWithoutSearchingOtherNoticeType(String type) throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"" + type + "Id\":123}]}");
        vips.enqueue(200, "{\"respCd\":0,\"results\":null}");
        var response = responseJson(request("GET", documentListPath(type), null, AUTH), 200);
        assertEquals(json.readTree("[]"), response);
        checkedNotice(type, "81000029");
        downstream("GET", "/documents?" + type + "Id=123");
    }

    @Test
    void missingTypedNoticeIs404AndFailedSearchIs502() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[]}");
        responseJson(request("GET", documentListPath("prohibition"), null, AUTH), 404);
        checkedNotice("prohibition", "81000029");
        vips.enqueue(200, "{\"respCd\":21}");
        responseJson(request("GET", documentListPath("impoundment"), null, AUTH), 502);
        checkedNotice("impoundment", "81000029");
    }

    @ParameterizedTest
    @ValueSource(strings = {"impoundment", "prohibition"})
    void typedDocumentListReturnsItemsAndSurfacesDownstreamFailure(String type) throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"" + type + "Id\":123}]}");
        vips.enqueue(200, "{\"respCd\":0,\"results\":[{\"documentId\":456,\"documentTypeCd\":\"MV2702A\"}]}");
        var response = responseJson(request("GET", documentListPath(type), null, AUTH), 200);
        assertTrue(response.isArray());
        assertEquals(1, response.size());
        assertEquals(456, response.at("/0/documentId").asInt());
        assertEquals("MV2702A", response.at("/0/documentTypeCd").asString());
        checkedNotice(type, "81000029");
        downstream("GET", "/documents?" + type + "Id=123");

        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"" + type + "Id\":123}]}");
        vips.enqueue(200, "{\"respCd\":21}");
        responseJson(request("GET", documentListPath(type), null, AUTH), 502);
        checkedNotice(type, "81000029");
        downstream("GET", "/documents?" + type + "Id=123");
    }

    @Test
    void documentTimestampsKeepTheOffsetVipsReturnedWithMilliseconds() throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"prohibitionId\":123}]}");
        vips.enqueue(200, """
                {"respCd":0,"results":[{"documentId":456,"documentTypeCd":"MV2702A",
                 "addToFileDtm":"2025-12-15T09:15:30.1-08:00",
                 "receivedDtm":"2026-11-05T16:15:30Z",
                 "disclosedToClientDtm":null}]}
                """);
        var response = responseJson(request("GET", documentListPath("prohibition"), null, AUTH), 200);
        assertEquals("2025-12-15T09:15:30.100-08:00", response.at("/0/addToFileDtm").asString());
        assertEquals("2026-11-05T16:15:30.000Z", response.at("/0/receivedDtm").asString());
        assertTrue(response.at("/0/disclosedToClientDtm").isNull());
        checkedNotice("prohibition", "81000029");
        downstream("GET", "/documents?prohibitionId=123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"impoundment", "prohibition"})
    void missingNoticeDoesNotUpload(String kind) throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[]}");
        responseJson(upload(kind, List.of(pdfPart(UnitTestUtilities.samplePdf())),
                Map.of("documentTypeCd", "MV2723"), AUTH), 404);
        checkedNotice(kind, "81000029");
    }

    @ParameterizedTest
    @CsvSource({"200,502", "503,503", "504,504"})
    void associationFailureDoesNotReturnTheUploadedIdOrRetryTheUpload(int vipsStatus, int status) throws Exception {
        queueNotice("prohibition", "IRP");
        vips.enqueue(201, "{\"status_code\":\"0\",\"document_id\":\"7654321\"}");
        vips.enqueue(vipsStatus, "{\"respCd\":1,\"respMsg\":\"association failed\"}");
        var response = upload(UnitTestUtilities.samplePdf());
        responseJson(response, status);
        assertFalse(response.body().contains("7654321"));
        assertTrue(response.headers().allValues("Location").isEmpty());
        assertEquals(status == 503 ? List.of("30") : List.of(), response.headers().allValues("Retry-After"));
        checkUploadNotice("prohibition");
        ords("POST", "/ords/storeDocument");
        checkAssociation("7654321", "IRP");
    }

    private void queueNotice(String kind, String noticeType) {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[{\"" + kind + "Id\":321}]}");
        vips.enqueue(200, "{\"respCd\":0,\"result\":{\"noticeTypeCd\":\"" + noticeType + "\"}}");
    }

    private void checkUploadNotice(String kind) throws Exception {
        checkedNotice(kind, "81000029");
        downstream("GET", "/cases/" + kind + "s/321");
    }

    private void checkAssociation(String id, String type) throws Exception {
        var association = downstream("POST", "/document-association/notice/" + id);
        var body = json.readTree(association.bodyBytes());
        assertEquals("81000029", body.path("noticeNo").asString());
        assertEquals(type, body.path("noticeTypeCd").asString());
    }

    private static String documentListPath(String type) {
        String resource = type.equals("impoundment") ? "impoundments" : "prohibitions";
        return "/" + resource + "/81000029/documents";
    }

    @Test
    void pingAndCodeTablesHaveDocumentedSuccessAndFailureResponses() throws Exception {
        vips.enqueue(200, "{\"status\":\"UP\"}");
        responseJson(request("GET", "/utility/ords/ping", null, AUTH), 200);
        ords("GET", "/ords/health");
        vips.enqueue(500, "{\"internal\":\"details\"}");
        responseJson(request("GET", "/utility/ords/ping", null, AUTH), 502);
        ords("GET", "/ords/health");

        vips.enqueue(200, "{\"respCd\":0,\"configuration\":{\"impoundLotOperatorDetails\":[]},\"respMsg\":\"Success\"}");
        responseJson(request("GET", "/codetables", null, AUTH), 200);
        downstream("GET", "/configuration?includeImpoundLotOperatorDetails=true");
        vips.enqueue(200, "{\"respCd\":1,\"respMsg\":\"Failure\"}");
        responseJson(request("GET", "/codetables", null, AUTH), 502);
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
        return upload(List.of(pdfPart(pdf)), Map.of("documentTypeCd", "MV2702A"));
    }

    private HttpResponse<String> upload(List<UploadPart> files, Map<String, String> fields) throws Exception {
        return upload("prohibition", files, fields, AUTH);
    }

    private HttpResponse<String> upload(String kind, List<UploadPart> files, Map<String, String> fields, String auth)
            throws Exception {
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
        return requestBytes("POST", documentListPath(kind), body.toByteArray(), auth,
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
