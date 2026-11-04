package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistrar;
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

    protected HttpResponse<String> request(String method, String path, String body, String auth)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/digitalforms-viirp/v1" + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
        if (auth != null) {
            builder.header("Authorization", auth);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        return client.send(builder.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    protected JsonNode responseJson(HttpResponse<String> response, int expectedStatus) {
        assertEquals(expectedStatus, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        return json.readTree(response.body());
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

    record RecordedRequest(String method, URI uri, Headers headers, String body) { }

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
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
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
