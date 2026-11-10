package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NoticeConflictsHttpTests extends HttpApiTestSupport {

    @ParameterizedTest
    @ValueSource(strings = {"prohibition", "impoundment"})
    void duplicateIs409WithoutCreating(String type) throws Exception {
        String notice = notice(type);
        vips.enqueue(200, found(type, notice));
        var response = responseJson(request("POST", "/" + type + "s", body(type), AUTH), 409);
        assertTrue(response.path("status_message").asString().contains(notice));
        checkedNotice(type, notice);
    }

    @ParameterizedTest
    @ValueSource(strings = {"prohibition", "impoundment"})
    void concurrentDuplicateIs409AfterRejectedCreate(String type) throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":1,\"respMsg\":\"Failure\"}");
        vips.enqueue(200, found(type, notice(type)));
        responseJson(request("POST", "/" + type + "s", body(type), AUTH), 409);
        checkedNotice(type, notice(type));
        downstream("POST", "/cases/" + type + "s");
        checkedNotice(type, notice(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"prohibition", "impoundment"})
    void genericDatabaseFailureIsNotAutomaticallyAConflict(String type) throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":1,\"respMsg\":\"ORA-sensitive detail\"}");
        availableNotice();
        var response = responseJson(request("POST", "/" + type + "s", body(type), AUTH), 500);
        assertEquals("Internal Server Error", response.path("status_message").asString());
        checkedNotice(type, notice(type));
        downstream("POST", "/cases/" + type + "s");
        checkedNotice(type, notice(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"prohibition", "impoundment"})
    void failedSearchMustNotAllowCreate(String type) throws Exception {
        vips.enqueue(200, "{\"respCd\":99,\"respMsg\":\"unavailable\"}");
        responseJson(request("POST", "/" + type + "s", body(type), AUTH), 500);
        checkedNotice(type, notice(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"prohibition", "impoundment"})
    void failedRecheckIs500NotAnUnconfirmedConflict(String type) throws Exception {
        availableNotice();
        vips.enqueue(200, "{\"respCd\":1}");
        vips.enqueue(200, "{\"respCd\":99}");
        responseJson(request("POST", "/" + type + "s", body(type), AUTH), 500);
        checkedNotice(type, notice(type));
        downstream("POST", "/cases/" + type + "s");
        checkedNotice(type, notice(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"prohibition", "impoundment"})
    void missingSearchResultMustNotAllowCreate(String type) throws Exception {
        vips.enqueue(200, "{\"respCd\":0,\"result\":null}");
        responseJson(request("POST", "/" + type + "s", body(type), AUTH), 500);
        checkedNotice(type, notice(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"prohibition", "impoundment"})
    void partialSearchMatchDoesNotBlockDifferentNotice(String type) throws Exception {
        vips.enqueue(200, found(type, notice(type) + "0"));
        String idProperty = type.equals("prohibition") ? "prohibitionId" : "impoundmentId";
        vips.enqueue(200, "{\"respCd\":0,\"" + idProperty + "\":123}");
        responseJson(request("POST", "/" + type + "s", body(type), AUTH), 201);
        checkedNotice(type, notice(type));
        downstream("POST", "/cases/" + type + "s");
    }

    @ParameterizedTest
    @ValueSource(strings = {"prohibition", "impoundment"})
    void unexpectedReadCodesAreNot200(String type) throws Exception {
        String path = "/" + type + "s/" + notice(type);
        vips.enqueue(200, "{\"respCd\":21}");
        responseJson(request("GET", path, null, AUTH), 500);
        checkedNotice(type, notice(type));

        vips.enqueue(200, found(type, notice(type)));
        vips.enqueue(200, "{\"respCd\":21}");
        responseJson(request("GET", path, null, AUTH), 500);
        checkedNotice(type, notice(type));
        downstream("GET", "/cases/" + type + "s/123");
    }

    private String body(String type) {
        return type.equals("prohibition") ? ProhibitionsHttpTests.BODY : ImpoundmentsHttpTests.BODY;
    }

    private String notice(String type) {
        return type.equals("prohibition") ? "21900601" : "00123456";
    }

    private String found(String type, String notice) {
        return "{\"respCd\":0,\"result\":[{\"" + type + "Id\":123,\"" + type + "NoticeNo\":\"" + notice + "\"}]}";
    }
}
