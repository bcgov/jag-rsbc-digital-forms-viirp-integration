package ca.bc.gov.open.digitalformsapi.viirp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.sun.net.httpserver.HttpServer;

import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;

/**
 * Exercises the VIPS WebClient retry filter against a real HTTP server that answers slower
 * than the client read timeout.
 */
class WebClientConfigurationTest {

	private static final int SLOW_RESPONSE_MS = 1000;

	private final AtomicInteger attempts = new AtomicInteger();
	private HttpServer server;
	private ExecutorService executor;

	@BeforeEach
	void startSlowServer() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		executor = Executors.newCachedThreadPool();
		server.setExecutor(executor);
		server.createContext("/", exchange -> {
			attempts.incrementAndGet();
			exchange.getRequestBody().readAllBytes();
			try {
				Thread.sleep(SLOW_RESPONSE_MS);
				exchange.sendResponseHeaders(200, -1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				exchange.close();
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
		executor.shutdownNow();
	}

	@Test
	void postIsNotResentAfterReadTimeout() {
		WebClient client = client(2, 5_000);

		assertThatThrownBy(() -> client.post().uri("/cases/prohibitions").bodyValue("{}")
				.retrieve().toBodilessEntity().block())
			.isInstanceOf(WebClientRequestException.class);

		assertThat(attempts).hasValue(1);
	}

	@Test
	void getIsRetriedAfterReadTimeout() {
		WebClient client = client(2, 5_000);

		assertThatThrownBy(() -> client.get().uri("/cases/prohibitions/search")
				.retrieve().toBodilessEntity().block())
			.isInstanceOf(DigitalFormsException.class)
			.hasMessageContaining("max attempts of: 2");

		assertThat(attempts).hasValue(3);
	}

	@Test
	void getRetriesStopAtMaxDuration() {
		WebClient client = client(20, 700);

		long start = System.nanoTime();
		assertThatThrownBy(() -> client.get().uri("/cases/prohibitions/search")
				.retrieve().toBodilessEntity().block())
			.isInstanceOf(DigitalFormsException.class)
			.hasMessageContaining("did not respond within 700 ms");
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;

		assertThat(elapsedMs).isLessThan(3_000);
		assertThat(attempts.get()).isLessThan(21);
	}

	private WebClient client(long retryCount, long maxDurationMs) {
		ConfigProperties properties = new ConfigProperties();
		properties.setVipsRestApiUrl("http://127.0.0.1:" + server.getAddress().getPort());
		properties.setVipsRestApiTimeout(200);
		properties.setVipsRestApiRetryCount(retryCount);
		properties.setVipsRestApiRetryDelay(0);
		properties.setVipsRestApiRetryMaxDuration(maxDurationMs);
		WebClientConfiguration configuration = new WebClientConfiguration();
		configuration.properties = properties;
		return configuration.vipsWebClient();
	}
}
