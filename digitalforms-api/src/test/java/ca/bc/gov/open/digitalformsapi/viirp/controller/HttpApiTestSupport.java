package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistrar;
import ca.bc.gov.open.digitalformsapi.viirp.filter.CorrelationIdFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.servlet.context-path=/digitalforms-viirp/v1",
        "digitalforms.basic-auth.user=route-test",
        "digitalforms.basic-auth.password=route-test-password",
        "vips.rest-api.username=vips-test",
        "vips.rest-api.password=vips-test-password",
        "vips.rest-api.credentials.guid=route-test-guid",
        "vips.rest-api.credentials.user=route-test-user",
        "vips.rest-api.credentials.displayname=Route Test",
        "vips.rest-api.timeout=2000",
        "vips.rest-api.retry.count=0",
        "vips.client.username=ords-test",
        "vips.client.password=ords-test-password"
})
@Import(HttpApiTestSupport.DownstreamConfiguration.class)
abstract class HttpApiTestSupport {

    private static final String CONTRACT = readContract();
    private static final OpenApiInteractionValidator VALIDATOR =
            OpenApiInteractionValidator.createForInlineApiSpecification(CONTRACT).build();
    protected static final JsonNode CONTRACT_JSON = JsonMapper.builder().build()
            .valueToTree(new Yaml(new SafeConstructor(new LoaderOptions())).load(CONTRACT));
    protected static final String AUTH = basicAuth("route-test", "route-test-password");
    protected final JsonMapper json = JsonMapper.builder().build();
    protected String correlationId;

    @LocalServerPort
    private int port;

    @Autowired
    protected VipsStub vips;

    private HttpClient client;

    @BeforeEach
    void setUpHttp() {
        vips.reset();
        correlationId = UUID.randomUUID().toString();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach
    void tearDownHttp() {
        try {
            assertTrue(vips.requests.isEmpty(), "Unexpected or unchecked downstream requests");
            assertTrue(vips.responses.isEmpty(), "Expected downstream calls were not made");
        } finally {
            client.close();
            vips.reset();
        }
    }

    protected String apiBaseUrl() {
        return "http://127.0.0.1:" + port + "/digitalforms-viirp/v1";
    }

    protected HttpResponse<String> request(String method, String path, String body, String auth)
            throws IOException, InterruptedException {
        return request(method, path, body, auth, body == null ? null : "application/json", "application/json");
    }

    protected HttpResponse<String> request(String method, String path, String body, String auth,
            String contentType, String accept) throws IOException, InterruptedException {
        HttpResponse<String> response = requestWithCorrelationIds(method, path, body, auth, contentType, accept,
                List.of(correlationId));
        assertEquals(List.of(correlationId), response.headers().allValues(CorrelationIdFilter.HEADER),
                "The supplied correlation ID must be echoed");
        return response;
    }

    /** Sends one {@value CorrelationIdFilter#HEADER} request header per supplied value (none if empty). */
    protected HttpResponse<String> requestWithCorrelationIds(String method, String path, String body, String auth,
            String contentType, String accept, List<String> correlationIds) throws IOException, InterruptedException {
        return sendRequest(method, path, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body), auth, contentType, accept, correlationIds);
    }

    protected HttpResponse<String> requestBytes(String method, String path, byte[] body, String auth,
            String contentType) throws IOException, InterruptedException {
        var response = sendRequest(method, path, HttpRequest.BodyPublishers.ofByteArray(body), auth,
                contentType, "application/json", List.of(correlationId));
        assertEquals(List.of(correlationId), response.headers().allValues(CorrelationIdFilter.HEADER));
        return response;
    }

    private HttpResponse<String> sendRequest(String method, String path, HttpRequest.BodyPublisher body,
            String auth, String contentType, String accept, List<String> correlationIds)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", accept);
        if (auth != null) {
            builder.header("Authorization", auth);
        }
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        for (String id : correlationIds) {
            builder.header(CorrelationIdFilter.HEADER, id);
        }
        HttpResponse<String> response = client.send(builder.method(method, body).build(),
                HttpResponse.BodyHandlers.ofString());
        // Unknown routes/methods have no OpenAPI operation; dedicated tests assert their 404/405 responses.
        if (hasOperation(method, path)) {
            assertConforms(method, path, response.statusCode(), response.body(), response.headers().map());
        }
        return response;
    }

    protected static void assertConforms(String method, String path, int status, String body,
            Map<String, List<String>> headers) {
        assertTrue(hasOperation(method, path), "No contract operation matches " + method + " " + path);
        SimpleResponse.Builder response = SimpleResponse.Builder.status(status).withBody(body);
        headers.forEach(response::withHeader);
        var report = VALIDATOR.validateResponse(path, Request.Method.valueOf(method), response.build());
        assertFalse(report.hasErrors(), report.getMessages().toString());
    }

    private static boolean hasOperation(String method, String path) {
        for (Map.Entry<String, JsonNode> entry : CONTRACT_JSON.path("paths").properties()) {
            String pattern = Pattern.quote(entry.getKey()).replaceAll("\\{[^}]+}", "\\\\E[^/]+\\\\Q");
            if (path.matches(pattern) && entry.getValue().has(method.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String readContract() {
        try {
            return new ClassPathResource("VI_IRP_V1.yaml").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load canonical API contract", e);
        }
    }

    protected JsonNode responseJson(HttpResponse<String> response, int expectedStatus) {
        assertEquals(expectedStatus, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        return json.readTree(response.body());
    }

    protected void availableNotice() {
        vips.enqueue(200, "{\"respCd\":0,\"result\":[]}");
    }

    protected void checkedNotice(String type, String notice) throws InterruptedException {
        downstream("GET", "/cases/" + type + "s/search?" + type + "NoticeNo=" + notice);
    }

    protected RecordedRequest downstream(String method, String path) throws InterruptedException {
        RecordedRequest request = vips.requests.poll(2, TimeUnit.SECONDS);
        assertNotNull(request, "Missing downstream request");
        assertEquals(method, request.method());
        assertEquals(path, request.uri().toString());
        assertEquals(basicAuth("vips-test", "vips-test-password"), request.headers().getFirst("Authorization"));
        assertEquals("route-test-guid", request.headers().getFirst("smgov_userguid"));
        assertEquals("route-test-user", request.headers().getFirst("sm_user"));
        assertEquals("Route Test", request.headers().getFirst("smgov_userdisplayname"));
        return request;
    }

    protected static String basicAuth(String user, String password) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    record RecordedRequest(String method, URI uri, Headers headers, byte[] bodyBytes) {
        String body() {
            return new String(bodyBytes, StandardCharsets.UTF_8);
        }
    }

    record StubResponse(int status, String body) { }

    @TestConfiguration(proxyBeanMethods = false)
    static class DownstreamConfiguration {
        @Bean(destroyMethod = "close")
        VipsStub vipsStub() throws IOException {
            return new VipsStub();
        }

        @Bean
        DynamicPropertyRegistrar downstreamProperties(VipsStub stub) {
            return registry -> {
                registry.add("vips.rest-api.url", stub::baseUrl);
                registry.add("vips.client.basePath", () -> stub.baseUrl() + "/ords");
            };
        }
    }

    // Only the remote VIPS boundary is stubbed; security, MVC, delegates and WebClient remain real.
    static class VipsStub implements AutoCloseable {
        final BlockingQueue<RecordedRequest> requests = new LinkedBlockingQueue<>();
        final BlockingQueue<StubResponse> responses = new LinkedBlockingQueue<>();
        private final HttpServer server;

        VipsStub() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void enqueue(int status, String body) {
            responses.add(new StubResponse(status, body));
        }

        void reset() {
            requests.clear();
            responses.clear();
        }

        private void handle(HttpExchange exchange) throws IOException {
            try (exchange) {
                requests.add(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI(),
                        exchange.getRequestHeaders(),
                        exchange.getRequestBody().readAllBytes()));
                StubResponse response = responses.poll();
                if (response == null) {
                    response = new StubResponse(500, "{\"error\":\"Unexpected downstream request\"}");
                }
                byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(response.status(), body.length);
                exchange.getResponseBody().write(body);
            }
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
