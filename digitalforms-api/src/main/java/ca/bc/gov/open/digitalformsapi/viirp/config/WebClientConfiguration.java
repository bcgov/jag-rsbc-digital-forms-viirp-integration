package ca.bc.gov.open.digitalformsapi.viirp.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Configuration
public class WebClientConfiguration {
	
	private final Logger logger = LoggerFactory.getLogger(WebClientConfiguration.class);
	
	@Autowired
    ConfigProperties properties;
	
	@Bean
    public WebClient vipsWebClient() {
        final var httpClient = HttpClient
                .create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getVipsRestApiTimeout())
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(properties.getVipsRestApiTimeout(), TimeUnit.MILLISECONDS));
                    connection.addHandlerLast(new WriteTimeoutHandler(properties.getVipsRestApiTimeout(), TimeUnit.MILLISECONDS));
                });

        return WebClient.builder()
                .baseUrl(properties.getVipsRestApiUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .filter(retryFilter())
                .build();
    }
	
	
	/**
	 * Connection retry configuration. Only safe methods (GET, HEAD) are retried, bounded by
	 * {@code vips.rest-api.retry.max-duration}. A POST that failed or timed out may already be
	 * committed in VIPS, so it is never re-sent.
	 * 
	 * @return {@link ExchangeFilterFunction}
	 */
	private ExchangeFilterFunction retryFilter() {
		return (request, next) -> {
			if (!HttpMethod.GET.equals(request.method()) && !HttpMethod.HEAD.equals(request.method())) {
				return next.exchange(request);
			}
			Mono<ClientResponse> response = next.exchange(request)
	        	.retryWhen(
		            Retry.fixedDelay(properties.getVipsRestApiRetryCount(), Duration.ofSeconds(properties.getVipsRestApiRetryDelay()))
		            .doAfterRetry(retrySignal -> logger.warn("Retrying VIPS {} {} (retry {}) after: {}",
		            		request.method(), request.url().getPath(), retrySignal.totalRetries() + 1, retrySignal.failure().toString()))
		            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
	                new DigitalFormsException(
	                    "VIPS API Service failed to respond, after max attempts of: "
	                    + retrySignal.totalRetries(), retrySignal.failure())));
			long maxDuration = properties.getVipsRestApiRetryMaxDuration();
			if (maxDuration <= 0) {
				return response;
			}
			return response
				.timeout(Duration.ofMillis(maxDuration))
				.onErrorMap(TimeoutException.class, e -> new DigitalFormsException(
					"VIPS API Service did not respond within " + maxDuration + " ms", e));
		};
	}
}
