package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import ca.bc.gov.open.digitalformsapi.viirp.filter.CorrelationIdFilter;
import tools.jackson.databind.JsonNode;

@TestPropertySource(properties = "springdoc.swagger-ui.enabled=true")
class ApiSurfaceHttpTests extends HttpApiTestSupport {

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE"})
    void removedDfPayloadOperationsReturn404WithoutDownstreamCalls(String method) throws Exception {
        String path = "/dfpayloads/21900601";
        String body = method.equals("POST") || method.equals("PUT") ? "{}" : null;
        responseJson(request(method, path, body, null), 401);
        JsonNode error = responseJson(request(method, path, body, AUTH), 404);
        assertEquals("Not Found", error.path("detail").asString());
    }

    @Test
    void publicDiscoveryRetainsSupportedRoutesAndExcludesDfPayloads() throws Exception {
        assertEquals("UP", responseJson(request("GET", "/actuator/health", null, null), 200).path("status").asString());
        JsonNode api = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        assertEquals(CONTRACT_JSON.path("openapi"), api.path("openapi"),
                "Runtime docs must use the contract's OpenAPI version; 3.1 deprecates the models' schema examples");
        assertEquals(CONTRACT_JSON.path("info"), api.path("info"), "API metadata must come from the contract");
        assertEquals(apiBaseUrl(), api.path("servers").path(0).path("url").asString(),
                "The server URL must be absolute so that clients such as Postman resolve it correctly");
        assertEquals(9, CONTRACT_JSON.path("paths").size(),
                "The canonical YAML remains the business API contract and excludes Actuator");
        JsonNode paths = api.path("paths");
        assertEquals(12, paths.size(),
                "Runtime docs should include the exposed Actuator endpoints and authenticated discovery: "
                        + paths.propertyNames());
        for (String route : new String[] {"prohibitions", "impoundments"}) {
            assertTrue(paths.path("/" + route).has("post"));
            assertTrue(paths.path("/" + route + "/{noticeNo}").has("get"));
        }
        assertFalse(paths.has("/documents"));
        assertTrue(paths.path("/documents/{documentId}").has("get"));
        var downloadResponse = paths.path("/documents/{documentId}").path("get")
                .path("responses").path("200").path("content").path("application/pdf").path("schema");
        assertEquals("string", downloadResponse.path("type").asString());
        assertEquals("binary", downloadResponse.path("format").asString());
        assertTrue(paths.path("/prohibitions/{noticeNo}/documents").has("get"));
        assertTrue(paths.path("/impoundments/{noticeNo}/documents").has("get"));
        assertFalse(paths.has("/prohibitions/{noticeNo}/documents/{documentId}"));
        assertFalse(paths.has("/impoundments/{noticeNo}/documents/{documentId}"));
        assertFalse(paths.has("/documents/list/{noticeNo}"));
        assertFalse(paths.has("/documents/association/notice/{documentId}"));
        assertTrue(paths.path("/codetables").has("get"));
        assertTrue(paths.path("/utility/ords/ping").has("get"));
        assertTrue(paths.path("/actuator/health").has("get"));
        assertTrue(paths.path("/actuator/info").has("get"));
        assertTrue(paths.path("/actuator").has("get"));
        assertTrue(paths.path("/actuator/health").path("get").path("security").isMissingNode()
                || paths.path("/actuator/health").path("get").path("security").isEmpty(),
                "Public health must not advertise authentication");
        for (String path : List.of("/actuator", "/actuator/info")) {
            var security = paths.path(path).path("get").path("security");
            assertEquals(1, security.size(), path + " must advertise authentication");
            assertTrue(security.get(0).has("oauth2"), path + " must use the API's OAuth2 scheme");
        }
        assertEquals(CONTRACT_JSON.at("/components/securitySchemes"), api.at("/components/securitySchemes"),
                "Clients such as Postman read the token URL from the published OAuth2 scheme");
        assertFalse(paths.has("/actuator/health/{*path}"), "Health component subpaths are not exposed");
        assertFalse(paths.has("/exception/{exception_id}"), "Test-only exception routes must not be published");
        assertFalse(api.toString().contains("{correlationId}"), "Correlation ID is a header, not a path variable");
        assertFalse(api.toString().toLowerCase(Locale.ROOT).contains("dfpayload"));
        responseJson(request("GET", "/documents/list/81000029", null, AUTH), 404);
        responseJson(request("POST", "/documents", "{}", AUTH), 404);
        for (String resource : List.of("prohibitions", "impoundments")) {
            responseJson(request("PUT", "/" + resource + "/81000029/documents/123", null, AUTH), 404);
        }
        responseJson(request("POST", "/documents/association/notice/123",
                "{\"noticeNo\":\"81000029\",\"noticeTypeCd\":\"IRP\"}", AUTH), 404);
        responseJson(request("GET", "/unknown", null, AUTH), 404);
        responseJson(request("GET", "/exception/forbidden", null, AUTH), 404);
    }

    @Test
    void healthOnlyReturnsStatusAndOptionalProbeGroupNamesEvenWhenAuthenticated() throws Exception {
        for (String authorization : new String[] {null, AUTH}) {
            var health = responseJson(request("GET", "/actuator/health", null, authorization), 200);
            assertEquals("UP", health.path("status").asString());
            assertTrue(Set.of("status", "groups").containsAll(health.propertyNames()), health.toString());
        }
        responseJson(request("GET", "/actuator/health/diskSpace", null, null), 401);
    }

    @Test
    void actuatorInformationAndDiscoveryRequireAuthentication() throws Exception {
        for (String path : List.of("/actuator/info", "/actuator")) {
            for (String authorization : new String[] {null, UNLISTED_CLIENT_AUTH}) {
                var response = request("GET", path, null, authorization);
                responseJson(response, 401);
                assertTrue(response.headers().firstValue("WWW-Authenticate").orElse("").startsWith("Bearer "));
                assertFalse(response.body().contains("\"build\""));
                assertFalse(response.body().contains("\"java\""));
            }
        }

        var properties = new Properties();
        try (var input = new ClassPathResource("META-INF/build-info.properties").getInputStream()) {
            properties.load(input);
        }
        var info = responseJson(request("GET", "/actuator/info", null, AUTH), 200);
        assertEquals(Set.of("build", "java"), info.propertyNames());
        assertEquals(properties.getProperty("build.version"), info.at("/build/version").asString());
        assertEquals(properties.getProperty("build.artifact"), info.at("/build/artifact").asString());
        assertEquals(System.getProperty("java.version"), info.at("/java/version").asString());
        assertEquals(System.getProperty("java.runtime.version"), info.at("/java/runtime/version").asString());
        assertEquals(System.getProperty("java.vm.version"), info.at("/java/jvm/version").asString());
    }

    @Test
    void onlyHealthAndInfoAreExposedAsActuatorEndpoints() throws Exception {
        var links = responseJson(request("GET", "/actuator", null, AUTH), 200).path("_links");
        assertEquals(Set.of("self", "health", "health-path", "info"), links.propertyNames());
        for (String path : List.of("/actuator/env", "/actuator/beans", "/actuator/metrics",
                "/actuator/configprops", "/actuator/heapdump")) {
            responseJson(request("GET", path, null, null), 401);
            responseJson(request("GET", path, null, AUTH), 404);
        }
    }

    @Test
    void swaggerUiLoadsThePublishedApiDocs() throws Exception {
        var yamlResponse = request("GET", "/v3/api-docs.yaml", null, null, null, "*/*");
        assertEquals(200, yamlResponse.statusCode());
        JsonNode yamlDocs = json.valueToTree(new Yaml(new SafeConstructor(new LoaderOptions())).load(yamlResponse.body()));
        assertEquals(CONTRACT_JSON.path("info"), yamlDocs.path("info"));
        var config = responseJson(request("GET", "/v3/api-docs/swagger-config", null, null), 200);
        assertEquals("/digitalforms-viirp/v1/v3/api-docs", config.path("url").asString());
        var ui = request("GET", "/swagger-ui/index.html", null, null, null, "text/html");
        assertEquals(200, ui.statusCode());
        assertTrue(ui.body().contains("swagger-ui"));
    }

    @Test
    void publishedUploadIsMultipartWithOneBinaryFileAndUnchangedResponse() throws Exception {
        var api = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        for (String path : List.of("/prohibitions/{noticeNo}/documents", "/impoundments/{noticeNo}/documents")) {
            var operation = api.path("paths").path(path).path("post");
            var content = operation.path("requestBody").path("content");
            assertEquals(1, content.size());
            var schema = content.path("multipart/form-data").path("schema");
            assertEquals("object", schema.path("type").asString());
            assertEquals("string", schema.path("properties").path("file").path("type").asString());
            assertEquals("binary", schema.path("properties").path("file").path("format").asString());
            assertTrue(schema.path("required").toString().contains("\"file\""));
            assertTrue(schema.path("required").toString().contains("\"documentTypeCd\""), schema.toPrettyString());
            assertEquals(Set.of("file", "documentTypeCd"), schema.path("properties").propertyNames());
            assertTrue(operation.path("responses").has("413"));
            var response = operation.path("responses").path("201").path("content")
                    .path("application/json").path("schema");
            assertEquals("#/components/schemas/CreateDocumentResponse", response.path("$ref").asString());
        }
    }

    @Test
    void publishedSuccessResponsesMatchResourcesAndCreatedLocations() throws Exception {
        var api = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        for (String resource : List.of("prohibitions", "impoundments")) {
            var path = "/" + resource + "/{noticeNo}";
            assertEquals(CONTRACT_JSON.path("paths").path(path).path("get")
                            .at("/responses/200/content/application~1json/schema"),
                    api.path("paths").path(path).path("get").at("/responses/200/content/application~1json/schema"));
            var collection = api.path("paths").path(path + "/documents").path("get")
                    .at("/responses/200/content/application~1json/schema");
            assertEquals("array", collection.path("type").asString());
            assertEquals("#/components/schemas/Document", collection.at("/items/$ref").asString());
            assertEquals(CONTRACT_JSON.path("paths").path(path + "/documents").path("get")
                    .at("/responses/200/content/application~1json/schema"), collection);
        }
        for (String resource : List.of("prohibitions", "impoundments",
                "prohibitions/{noticeNo}/documents", "impoundments/{noticeNo}/documents")) {
            var created = api.path("paths").path("/" + resource).path("post").at("/responses/201");
            assertEquals(CONTRACT_JSON.at("/components/headers/Location"), created.at("/headers/Location"));
        }
        for (String model : List.of("CreateProhibitionResponse", "CreateImpoundmentResponse",
                "CodeTablesResponse")) {
            var canonical = CONTRACT_JSON.path("components").path("schemas").path(model);
            var published = api.path("components").path("schemas").path(model);
            assertEquals(canonical.path("properties").propertyNames(), published.path("properties").propertyNames());
            assertEquals(canonical.path("required"), published.path("required"));
            assertFalse(published.path("properties").has("respMsg"));
            assertFalse(published.path("properties").has("warnings"));
            assertFalse(canonical.path("properties").has("warnings"));
        }
        for (String removed : List.of("GetProhibitionServiceResponse", "GetImpoundmentServiceResponse",
                "GetDocumentsListServiceResponse", "Warning")) {
            assertFalse(api.path("components").path("schemas").has(removed));
            assertFalse(CONTRACT_JSON.path("components").path("schemas").has(removed));
        }
    }

    @Test
    void publishedDatesAndTimestampsUseTheContractFormats() throws Exception {
        var api = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        int temporalFields = 0;
        for (var schema : CONTRACT_JSON.path("components").path("schemas").properties()) {
            for (var property : schema.getValue().path("properties").properties()) {
                var canonical = property.getValue();
                String format = canonical.path("format").asString();
                if (!format.equals("date") && !format.equals("date-time")) {
                    continue;
                }
                temporalFields++;
                String field = schema.getKey() + "." + property.getKey();
                var published = api.path("components").path("schemas").path(schema.getKey())
                        .path("properties").path(property.getKey());
                assertEquals("string", canonical.path("type").asString(), field);
                assertEquals("string", published.path("type").asString(), field);
                assertEquals(format, published.path("format").asString(), field);
                assertFalse(canonical.has("pattern") || published.has("pattern"),
                        field + " must rely on its format rather than a legacy timestamp pattern");
            }
        }
        assertEquals(15, temporalFields, "Every VIPS date and timestamp field must declare its format");
    }

    @Test
    void publishedProhibitionSchemasOmitRetiredFields() throws Exception {
        var api = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        for (String model : List.of("ProhibitionCreate", "Prohibition")) {
            for (JsonNode contract : List.of(CONTRACT_JSON, api)) {
                var properties = contract.path("components").path("schemas").path(model).path("properties");
                for (String field : List.of("cityNm", "provinceCd", "streetDetailsTxt",
                        "policeOfficerNm", "policeOfficerNo", "expiryDt", "tempDlEffectiveDt", "tempDlExpiryDt")) {
                    assertFalse(properties.has(field), model + "." + field);
                }
            }
        }
    }

    @Test
    void publishedCreateSchemasOmitServerOwnedInputsButReadSchemasKeepNoticeCodes() throws Exception {
        var api = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        var removed = Map.of(
                "CreateImpoundment", List.of("priorImpoundmentIds", "documentIds"),
                "CreateProhibition", List.of("documentIds"),
                "ImpoundmentCreate", List.of("noticeTypeCd", "noticeSubjectCd"),
                "ProhibitionCreate", List.of("noticeSubjectCd"));
        for (JsonNode contract : List.of(CONTRACT_JSON, api)) {
            for (var entry : removed.entrySet()) {
                var schema = contract.path("components").path("schemas").path(entry.getKey());
                assertTrue(schema.isObject(), entry.getKey());
                for (String field : entry.getValue()) {
                    assertFalse(schema.path("properties").has(field), entry.getKey() + "." + field);
                    assertFalse(schema.path("required").toString().contains("\"" + field + "\""));
                }
            }
            for (String readModel : List.of("Prohibition", "Impoundment")) {
                var properties = contract.path("components").path("schemas").path(readModel).path("properties");
                assertEquals("string", properties.path("noticeTypeCd").path("type").asString());
                assertEquals("string", properties.path("noticeSubjectCd").path("type").asString());
                assertFalse(properties.path("noticeTypeCd").has("enum"));
                assertFalse(properties.path("noticeSubjectCd").has("enum"));
            }
        }
    }

    @Test
    void documentedRequestEnumsArePublishedForJson() throws Exception {
        var api = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        for (var entry : Map.of(
                "/components/schemas/ProhibitionCreate/properties/noticeTypeCd", List.of("UL", "IRP", "ADP"),
                "/components/schemas/AddressCreate/properties/registrationAddressTypeCd",
                        List.of("RES", "MAIL", "BUSI")).entrySet()) {
            for (JsonNode contract : List.of(CONTRACT_JSON, api)) {
                assertEquals(json.valueToTree(entry.getValue()), contract.at(entry.getKey()).path("enum"),
                        entry.getKey());
            }
        }
    }

    @Test
    void codeTablesReturnConfigurationWithoutUpstreamStatusStrings() throws Exception {
        vips.enqueue(200, """
                {"configuration":{"countries":[{"objectCd":"CAN","objectDsc":"Canada","activeYN":"Y"}]},
                 "respMsg":"sensitive upstream status text"}
                """);
        var response = responseJson(request("GET", "/codetables", null, AUTH), 200);
        assertEquals(Set.of("configuration"), response.propertyNames());
        assertEquals("CAN", response.at("/configuration/countries/0/objectCd").asString());
        assertEquals("Canada", response.at("/configuration/countries/0/objectDsc").asString());
        var upstream = vips.requests.poll(2, TimeUnit.SECONDS);
        assertNotNull(upstream);
        assertEquals("GET", upstream.method());
        assertEquals("/configuration", upstream.uri().toString());
        assertEquals(basicAuth("vips-test", "vips-test-password"), upstream.headers().getFirst("Authorization"));
    }

    @Test
    void codeTablesOnlyReturnActiveEntriesAndHideActiveFlags() throws Exception {
        vips.enqueue(200, """
                {"respCd":0,"configuration":{
                 "data_sources":[{"objectCd":"VIPS","objectDsc":"VIPS","activeYN":"Y","internalYN":"N"}],
                 "registration_roles":[{"objectCd":"REGOWN","objectDsc":"Owner","activeYN":"N","internalYN":"Y"}],
                 "policeDetachments":[{"identityId":304,"identityNm":"Victoria","activeYN":"Y"},
                    {"identityId":305,"activeYN":"N"},{"identityId":306,"activeYN":null},{"identityId":307}],
                 "originalCauses":[{"noticeTypeCd":"IRP","noticeActiveYN":"Y",
                  "codes":[{"objectCd":"YES","objectAbbrev":"Y","activeYN":"Y"},
                           {"objectCd":"NO","activeYN":"N"},{"objectCd":"UNKNOWN","activeYN":null}]},
                  {"noticeTypeCd":"ADP","noticeActiveYN":"N","codes":[{"objectCd":"HIDDEN","activeYN":"Y"}]},
                  {"noticeTypeCd":"UL","noticeActiveYN":null,"codes":[{"objectCd":"HIDDEN","activeYN":"Y"}]}],
                 "documentNotices":[{"documentTypeCd":"MV2702A","documentActiveYN":"N"}],
                 "groundsDecisions":[{"reviewAppTypeCd":"IRP","reviewAppActiveYN":"Y",
                    "codes":[{"objectCd":"YES","activeYN":"Y"},{"objectCd":"NO","activeYN":"N"}]},
                    {"reviewAppTypeCd":"INACTIVE","reviewAppActiveYN":"N","codes":[]}],
                 "countries":[{"objectCd":"CAN","activeYN":null}],
                 "iloDocumentTypes":[{"objectCd":"ignored"}]}}
                """);
        var response = responseJson(request("GET", "/codetables", null, AUTH), 200);
        var configuration = response.path("configuration");
        var no = json.readTree("false");
        assertEquals(no, configuration.at("/dataSources/0/internal"));
        assertEquals(0, configuration.path("registrationRoles").size());
        assertEquals(1, configuration.path("policeDetachments").size());
        assertEquals(304, configuration.at("/policeDetachments/0/identityId").asInt());
        assertEquals(1, configuration.path("originalCauses").size());
        assertEquals("YES", configuration.at("/originalCauses/0/codes/0/objectCd").asString());
        assertEquals(1, configuration.at("/originalCauses/0/codes").size());
        assertEquals(0, configuration.path("documentNotices").size());
        assertEquals(1, configuration.path("groundsDecisions").size());
        assertEquals(1, configuration.at("/groundsDecisions/0/codes").size());
        assertEquals(0, configuration.path("countries").size());
        for (String vipsName : List.of("data_sources", "registration_roles", "YN\"", "iloDocumentTypes",
                "\"active\"", "\"noticeActive\"", "\"documentActive\"", "\"reviewAppActive\"")) {
            assertFalse(response.toString().contains(vipsName), vipsName);
        }
        assertNotNull(vips.requests.poll(2, TimeUnit.SECONDS));
    }

    @Test
    void codeTableActivityFlagsAreAbsentFromThePublishedContract() throws Exception {
        JsonNode runtime = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        for (JsonNode contract : List.of(CONTRACT_JSON, runtime)) {
            for (String schema : List.of("CodeDescription", "AbbreviatedCodeDescription", "IdentityName",
                    "NoticeTypeCodes", "NoticeTypeAbbreviatedCodes", "DocumentTypeCodes",
                    "ReviewApplicationTypeCodes")) {
                var properties = contract.at("/components/schemas/" + schema + "/properties");
                assertTrue(properties.isObject(), schema);
                for (String flag : List.of("active", "noticeActive", "documentActive", "reviewAppActive")) {
                    assertFalse(properties.has(flag), schema + "." + flag);
                }
            }
        }
    }

    @Test
    void everyPublishedCodeTableFiltersRowsAndGroups() throws Exception {
        var upstreamConfiguration = json.createObjectNode();
        var schemas = CONTRACT_JSON.at("/components/schemas/CodeTables/properties");
        for (var table : schemas.properties()) {
            String upstreamName = switch (table.getKey()) {
                case "dataSources" -> "data_sources";
                case "registrationRoles" -> "registration_roles";
                default -> table.getKey();
            };
            String type = table.getValue().at("/items/$ref").asString();
            String flag = switch (type.substring(type.lastIndexOf('/') + 1)) {
                case "NoticeTypeCodes", "NoticeTypeAbbreviatedCodes" -> "noticeActiveYN";
                case "DocumentTypeCodes" -> "documentActiveYN";
                case "ReviewApplicationTypeCodes" -> "reviewAppActiveYN";
                default -> "activeYN";
            };
            boolean grouped = !flag.equals("activeYN");
            var active = json.createObjectNode().put(flag, "Y");
            if (grouped) {
                active.putArray("codes")
                        .addObject().put("objectCd", "KEEP").put("activeYN", "Y");
                active.withArray("codes").addObject().put("objectCd", "DROP").put("activeYN", "N");
                active.withArray("codes").addObject().put("objectCd", "UNKNOWN");
            } else if (type.endsWith("/IdentityName")) {
                active.put("identityId", 42);
            } else {
                active.put("objectCd", "KEEP");
            }
            upstreamConfiguration.putArray(upstreamName)
                    .add(active)
                    .add(json.createObjectNode().put(flag, "N"))
                    .add(json.createObjectNode().putNull(flag))
                    .add(json.createObjectNode())
                    .addNull();
        }
        vips.enqueue(200, json.createObjectNode().set("configuration", upstreamConfiguration).toString());
        var configuration = responseJson(request("GET", "/codetables", null, AUTH), 200).path("configuration");
        for (var table : schemas.properties()) {
            var entries = configuration.path(table.getKey());
            assertEquals(1, entries.size(), table.getKey());
            String type = table.getValue().at("/items/$ref").asString();
            if (type.endsWith("/IdentityName")) {
                assertEquals(42, entries.get(0).path("identityId").asInt(), table.getKey());
            } else if (type.endsWith("/CodeDescription")) {
                assertEquals("KEEP", entries.get(0).path("objectCd").asString(), table.getKey());
            } else {
                assertEquals(1, entries.get(0).path("codes").size(), table.getKey());
                assertEquals("KEEP", entries.get(0).path("codes").get(0).path("objectCd").asString(), table.getKey());
            }
        }
        assertNotNull(vips.requests.poll(2, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"documents\":[{\"noticeActiveYN\":\"bad\",\"codes\":[]}]}",
            "{\"documentNotices\":[{\"documentActiveYN\":true,\"codes\":[]}]}",
            "{\"groundsDecisions\":[{\"reviewAppActiveYN\":1,\"codes\":[]}]}",
            "{\"documents\":[{\"noticeActiveYN\":\"Y\",\"codes\":[{\"activeYN\":\"bad\"}]}]}",
            "{\"countries\":{}}",
            "{\"documents\":[{\"noticeActiveYN\":\"Y\",\"codes\":{}}]}"
    })
    void malformedCodeTableActivityOrArraysFailAsUpstreamErrors(String configuration) throws Exception {
        vips.enqueue(200, "{\"configuration\":" + configuration + "}");
        responseJson(request("GET", "/codetables", null, AUTH), 502);
        assertNotNull(vips.requests.poll(2, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"y\"", "\"Yes\"", "\"\"", "true", "1"})
    void invalidCodeTableFlagIsAnInvalidUpstreamResponse(String flag) throws Exception {
        vips.enqueue(200, "{\"configuration\":{\"countries\":[{\"objectCd\":\"CAN\",\"activeYN\":%s}]}}"
                .formatted(flag));
        var error = responseJson(request("GET", "/codetables", null, AUTH), 502);
        assertEquals("Invalid response from VIPS.", error.path("detail").asString());
        assertNotNull(vips.requests.poll(2, TimeUnit.SECONDS));
    }

    @Test
    void publishedErrorResponsesIncludeTheStructuredProblemContract() throws Exception {
        var api = responseJson(request("GET", "/v3/api-docs", null, null), 200);
        for (var path : CONTRACT_JSON.path("paths").properties()) {
            for (var operation : path.getValue().properties()) {
                for (String status : operation.getValue().path("responses").propertyNames()) {
                    var response = api.path("paths").path(path.getKey()).path(operation.getKey())
                            .path("responses").path(status);
                    var content = response.path("content");
                    if ("503".equals(status)) {
                        var header = response.path("headers").path("Retry-After");
                        assertTrue(header.path("required").asBoolean());
                        assertEquals("string", header.path("schema").path("type").asString());
                        assertEquals(CONTRACT_JSON.at("/components/responses/503ServiceUnavailable/headers/Retry-After"),
                                header);
                    }
                    if (Integer.parseInt(status) < 400) {
                        assertFalse(content.has("application/problem+json"),
                                "Successful responses must not advertise the error media type");
                        continue;
                    }
                    assertEquals(Set.of("application/problem+json"), content.propertyNames(),
                            operation.getKey() + " " + path.getKey() + " " + status);
                    assertEquals("#/components/schemas/Problem",
                            content.at("/application~1problem+json/schema/$ref").asString());
                }
            }
        }
        for (String name : List.of("Problem", "ValidationError")) {
            var schema = api.path("components").path("schemas").path(name);
            var canonical = CONTRACT_JSON.path("components").path("schemas").path(name);
            assertEquals(canonical.path("properties").propertyNames(), schema.path("properties").propertyNames());
            for (var required : canonical.path("required")) {
                assertTrue(schema.path("required").toString().contains(required.toString()),
                        name + " must require " + required);
            }
        }
        assertEquals("#/components/schemas/ValidationError",
                api.at("/components/schemas/Problem/properties/errors/items/$ref").asString());
        assertFalse(api.toString().contains("status_message"));
    }

    @Test
    void contractGuardRejectsWrongStatusSchemaContentTypeAndCorrelationHeader() {
        String path = "/prohibitions";
        String ok = "{\"prohibitionId\":123456789012345}";
        assertConforms("POST", path, 201, ok, createHeaders("application/json", correlationId));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 200, ok, createHeaders("application/json", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, "{\"prohibitionId\":\"not-an-id\"}",
                        createHeaders("application/json", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, "{}", createHeaders("application/json", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 500, "{}", headers("application/problem+json", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, ok, createHeaders("text/plain", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, ok, createHeaders("application/json", null)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, ok, createHeaders("application/json", "not valid")));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 201, ok, headers("application/json", correlationId)));
        String problem = """
                {"type":"about:blank","title":"Bad Request","status":400,"detail":"Request validation failed.",
                 "instance":"/digitalforms-viirp/v1/prohibitions",
                 "errors":[{"field":"prohibition","message":"must not be null"}]}
                """;
        assertConforms("POST", path, 400, problem, headers("application/problem+json", correlationId));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 400, problem, headers("application/json", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 400, "{\"status_message\":\"Bad Request\"}",
                        headers("application/problem+json", correlationId)));
        assertThrows(AssertionError.class,
                () -> assertConforms("POST", path, 400, problem.replace("\"field\":\"prohibition\",", ""),
                        headers("application/problem+json", correlationId)));
    }

    private static Map<String, List<String>> headers(String contentType, String correlationId) {
        return correlationId == null
                ? Map.of("Content-Type", List.of(contentType))
                : Map.of("Content-Type", List.of(contentType), CorrelationIdFilter.HEADER, List.of(correlationId));
    }

    private static Map<String, List<String>> createHeaders(String contentType, String correlationId) {
        var headers = new HashMap<>(headers(contentType, correlationId));
        headers.put("Location", List.of("/digitalforms-viirp/v1/prohibitions/21900601"));
        return headers;
    }
}
