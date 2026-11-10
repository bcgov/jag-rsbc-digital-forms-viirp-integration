package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

class CreateRequestValidationHttpTests extends HttpApiTestSupport {

    private static final List<String> ROUTES = List.of("/prohibitions", "/impoundments");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final long CREATED_ID = 123456789012345L;
    private static final List<Mutation> INVALID_STRINGS = List.of(
            Mutation.MISSING, Mutation.NULL, Mutation.EMPTY_STRING,
            Mutation.SPACE_ONLY, Mutation.TAB_ONLY, Mutation.NEWLINE_ONLY);

    @ParameterizedTest(name = "{0} {1}: {2}")
    @MethodSource("invalidRequiredFields")
    void requiredFieldIsRejectedBeforeCallingVips(String route, String pointer, Mutation mutation) throws Exception {
        ObjectNode body = validBody(route);
        mutate(body, pointer, mutation);
        String field = pointer.substring(1).replaceAll("/(\\d+)", "[$1]").replace('/', '.');
        assertRejected(route, body, field + (mutation == Mutation.EMPTY_OBJECT ? "." : ":"));
    }

    static Stream<Arguments> invalidRequiredFields() {
        return ROUTES.stream().flatMap(route -> requiredFields(route).stream())
                .flatMap(field -> {
                    List<Mutation> mutations = new ArrayList<>(List.of(Mutation.MISSING, Mutation.NULL));
                    switch (field.schema().path("type").asString()) {
                        case "string" -> mutations = INVALID_STRINGS;
                        case "object" -> {
                            if (!field.schema().path("required").isEmpty()) {
                                mutations.add(Mutation.EMPTY_OBJECT);
                            }
                        }
                        case "array" -> {
                            if (field.schema().path("minItems").asInt(0) > 0) {
                                mutations.add(Mutation.EMPTY_ARRAY);
                            }
                        }
                        default -> { }
                    }
                    return mutations.stream().map(mutation -> Arguments.of(field.route(), field.pointer(), mutation));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"/prohibitions", "/impoundments"})
    void validControlRequestReachesVips(String route) throws Exception {
        assertAccepted(route, validBody(route));
    }

    @ParameterizedTest(name = "{0} {1}: empty array is allowed")
    @MethodSource("emptyAllowedArrays")
    void requiredDoesNotMeanNonemptyUnlessTheContractSaysSo(String route, String pointer) throws Exception {
        ObjectNode body = validBody(route);
        mutate(body, pointer, Mutation.EMPTY_ARRAY);
        assertEquals(JSON.createArrayNode(), assertAccepted(route, body).at(pointer));
    }

    static Stream<Arguments> emptyAllowedArrays() {
        return ROUTES.stream().flatMap(route -> requiredFields(route).stream())
                .filter(field -> field.schema().path("type").asString().equals("array")
                        && field.schema().path("minItems").asInt(0) == 0)
                .map(field -> Arguments.of(field.route(), field.pointer()));
    }

    @ParameterizedTest(name = "{0} {1}: {2} licence object is allowed")
    @MethodSource("optionalLicenceObjects")
    void licenceObjectsRemainOptional(String route, String pointer, Mutation mutation) throws Exception {
        ObjectNode body = validBody(route);
        mutate(body, pointer, mutation);
        assertTrue(assertAccepted(route, body).at(pointer).isNull());
    }

    static Stream<Arguments> optionalLicenceObjects() {
        return ROUTES.stream().flatMap(route -> registrationPointers(route).stream()
                .flatMap(pointer -> Stream.of(Mutation.MISSING, Mutation.NULL)
                        .map(mutation -> Arguments.of(route, pointer + "/vipsLicenceObj", mutation))));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidImpoundmentLicences")
    void suppliedImpoundmentLicenceRequiresANumberInEveryRegistration(String pointer, Mutation mutation)
            throws Exception {
        ObjectNode body = validBody("/impoundments");
        mutate(body, pointer, mutation);
        assertRejected("/impoundments", body, "Invalid license object - DL is a required field.");
    }

    static Stream<Arguments> invalidImpoundmentLicences() {
        return registrationPointers("/impoundments").stream().flatMap(pointer -> Stream.concat(
                INVALID_STRINGS.stream().map(mutation ->
                        Arguments.of(pointer + "/vipsLicenceObj/driverLicenceNo", mutation)),
                Stream.of(Arguments.of(pointer + "/vipsLicenceObj", Mutation.EMPTY_OBJECT))));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void nullImpoundmentRegistrationIsRejectedAtEveryPosition(int index) throws Exception {
        ObjectNode body = validBody("/impoundments");
        body.withArray("vipsRegistrationCreateArray").set(index, NullNode.getInstance());
        assertRejected("/impoundments", body, "vipsRegistrationCreateArray must not contain null entries.");
    }

    private void assertRejected(String route, ObjectNode body, String expectedMessage) throws Exception {
        JsonNode error = responseJson(request("POST", route, body.toString(), AUTH), 400);
        assertTrue(error.path("status_message").asString().contains(expectedMessage), error.toString());
        assertTrue(vips.requests.isEmpty(), "Invalid requests must not reach VIPS, including the notice lookup");
    }

    private JsonNode assertAccepted(String route, ObjectNode body) throws Exception {
        String type = route.substring(1, route.length() - 1);
        availableNotice();
        vips.enqueue(200, JSON.createObjectNode().put("respCd", 0).put(type + "Id", CREATED_ID).toString());
        JsonNode response = responseJson(request("POST", route, body.toString(), AUTH), 201);
        assertEquals(CREATED_ID, response.path(type + "Id").asLong());
        String noticePointer = route.equals("/prohibitions")
                ? "/vipsProhibitionCreate/prohibitionNoticeNo" : "/vipsImpoundCreate/impoundmentNoticeNo";
        checkedNotice(type, body.at(noticePointer).asString());
        return json.readTree(downstream("POST", "/cases" + route).body());
    }

    private static List<RequiredField> requiredFields(String route) {
        JsonNode schema = CONTRACT_JSON.path("paths").path(route).path("post").path("requestBody")
                .path("content").path("application/json").path("schema");
        List<RequiredField> fields = new ArrayList<>();
        collectRequiredFields(route, schema, validBody(route), "", fields);
        assertFalse(fields.isEmpty(), "No required fields discovered for " + route);
        return fields;
    }

    private static void collectRequiredFields(String route, JsonNode schema, JsonNode sample, String pointer,
            List<RequiredField> fields) {
        schema = resolveSchema(schema);
        if (schema.path("type").asString().equals("array")) {
            ArrayNode array = assertInstanceOf(ArrayNode.class, sample, "Fixture must populate " + route + pointer);
            assertFalse(array.isEmpty(), "Fixture needs object-array entries to exercise " + route + pointer);
            for (int i = 0; i < array.size(); i++) {
                collectRequiredFields(route, schema.path("items"), array.get(i), pointer + "/" + i, fields);
            }
            return;
        }

        assertEquals("object", schema.path("type").asString(), "Expected an object schema at " + route + pointer);
        ObjectNode object = assertInstanceOf(ObjectNode.class, sample, "Fixture must populate " + route + pointer);
        JsonNode properties = schema.path("properties");
        for (JsonNode required : schema.path("required")) {
            String name = required.asString();
            assertTrue(object.hasNonNull(name), "Fixture is missing required field " + route + pointer + "/" + name);
            fields.add(new RequiredField(route, pointer + "/" + name, resolveSchema(properties.path(name))));
        }
        for (var property : properties.properties()) {
            JsonNode childSchema = resolveSchema(property.getValue());
            String type = childSchema.path("type").asString();
            if (type.equals("object") || (type.equals("array")
                    && resolveSchema(childSchema.path("items")).path("type").asString().equals("object"))) {
                collectRequiredFields(route, childSchema, object.path(property.getKey()),
                        pointer + "/" + property.getKey(), fields);
            }
        }
    }

    private static JsonNode resolveSchema(JsonNode schema) {
        if (schema.has("$ref")) {
            String ref = schema.path("$ref").asString();
            assertTrue(ref.startsWith("#/"), "Expected a local contract reference: " + ref);
            schema = CONTRACT_JSON.at(ref.substring(1));
        }
        assertTrue(schema.isObject(), "Missing contract schema");
        return schema;
    }

    private static ObjectNode validBody(String route) {
        String fixture = switch (route) {
            case "/prohibitions" -> ProhibitionsHttpTests.BODY;
            case "/impoundments" -> ImpoundmentsHttpTests.BODY;
            default -> throw new IllegalArgumentException("Unknown create route: " + route);
        };
        ObjectNode body = JSON.readValue(fixture, ObjectNode.class);
        if (route.equals("/prohibitions")) {
            addAddresses(assertInstanceOf(ObjectNode.class, body.path("vipsRegistrationCreate")));
        } else {
            ArrayNode registrations = body.withArray("vipsRegistrationCreateArray");
            ObjectNode second = assertInstanceOf(ObjectNode.class, registrations.get(0)).deepCopy();
            second.put("registrationRoleCd", "DRVR").put("surnameNm", "SECOND HTTP TEST");
            registrations.add(second);
            for (JsonNode registration : registrations) {
                addAddresses(assertInstanceOf(ObjectNode.class, registration));
            }
        }
        return body;
    }

    private static void addAddresses(ObjectNode registration) {
        ArrayNode addresses = registration.putArray("vipsAddressArray");
        addresses.addObject().put("registrationAddressTypeCd", "RES");
        addresses.addObject().put("registrationAddressTypeCd", "MAIL");
    }

    private static List<String> registrationPointers(String route) {
        return route.equals("/prohibitions") ? List.of("/vipsRegistrationCreate")
                : IntStream.range(0, validBody(route).path("vipsRegistrationCreateArray").size())
                        .mapToObj(i -> "/vipsRegistrationCreateArray/" + i).toList();
    }

    private static void mutate(ObjectNode body, String pointer, Mutation mutation) {
        int separator = pointer.lastIndexOf('/');
        ObjectNode parent = assertInstanceOf(ObjectNode.class, body.at(pointer.substring(0, separator)));
        String name = pointer.substring(separator + 1);
        assertTrue(parent.has(name), "Cannot mutate missing fixture property: " + pointer);
        switch (mutation) {
            case MISSING -> parent.remove(name);
            case NULL -> parent.putNull(name);
            case EMPTY_STRING -> parent.put(name, "");
            case SPACE_ONLY -> parent.put(name, " ");
            case TAB_ONLY -> parent.put(name, "\t");
            case NEWLINE_ONLY -> parent.put(name, "\n");
            case EMPTY_OBJECT -> parent.putObject(name);
            case EMPTY_ARRAY -> parent.putArray(name);
        }
    }

    private record RequiredField(String route, String pointer, JsonNode schema) { }

    enum Mutation {
        MISSING, NULL, EMPTY_STRING, SPACE_ONLY, TAB_ONLY, NEWLINE_ONLY, EMPTY_OBJECT, EMPTY_ARRAY
    }
}
