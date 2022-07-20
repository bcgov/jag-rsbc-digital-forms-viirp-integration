package ca.bc.gov.open.digitalformsapi.viirp.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Configuration
public class WebClientConfiguration {
	
	private final Logger logger = LoggerFactory.getLogger(WebClientConfiguration.class);
	
	@Autowired
    DigitalFormsConstants constants;
	
	@Bean
    public WebClient vipsWebClient() {
        final var httpClient = HttpClient
                .create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, constants.getVipsRestApiTimeout())
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(constants.getVipsRestApiTimeout(), TimeUnit.MILLISECONDS));
                    connection.addHandlerLast(new WriteTimeoutHandler(constants.getVipsRestApiTimeout(), TimeUnit.MILLISECONDS));
                });

        return WebClient.builder()
                .baseUrl(constants.getVipsRestApiUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .filter(retryFilter())
                .build();
    }
	
	
	/**
	 * Connection retry configuration
	 * 
	 * @return {@link ExchangeFilterFunction}
	 */
	private ExchangeFilterFunction retryFilter() {
		return (request, next) ->
			next.exchange(request)
	        	.retryWhen(
		            Retry.fixedDelay(constants.getVipsRestApiRetryCount(), Duration.ofSeconds(constants.getVipsRestApiRetryDelay()))
		            .doAfterRetry(retrySignal -> {
	                    logger.info("Retried " + retrySignal.totalRetries());
	                })
		            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
	                new DigitalFormsException(
	                    "VIPS API Service failed to respond, after max attempts of: "
	                    + retrySignal.totalRetries())));
	}
}
