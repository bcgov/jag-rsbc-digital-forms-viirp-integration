package ca.bc.gov.open.digitalformsapi.viirp.exception;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiException;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.codec.DecoderException;

public class UpstreamServiceException extends DigitalFormsException {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(UpstreamServiceException.class);
    private static final String DEFAULT_RETRY_AFTER = "30";

    private final HttpStatus status;
    private final String retryAfter;

    private UpstreamServiceException(HttpStatus status, String message, Throwable cause, String retryAfter) {
        super(message, cause);
        this.status = status;
        this.retryAfter = retryAfter;
    }

    public static UpstreamServiceException invalidResponse(String message) {
        return invalidResponse(message, null);
    }

    public static UpstreamServiceException invalidResponse(String message, Throwable cause) {
        return new UpstreamServiceException(HttpStatus.BAD_GATEWAY, message, cause, null);
    }

    /** Call only at a VIPS WS/ORDS boundary, never for arbitrary application exceptions. */
    public static RuntimeException from(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof UpstreamServiceException upstream) {
                return upstream;
            }
            if (cause instanceof EncodingException) {
                return localFailure(failure);
            }
            if (cause instanceof SocketTimeoutException || cause instanceof TimeoutException
                    || cause instanceof ConnectTimeoutException
                    || cause instanceof io.netty.handler.timeout.TimeoutException) {
                return new UpstreamServiceException(HttpStatus.GATEWAY_TIMEOUT,
                        "VIPS request timed out.", failure, null);
            }
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof WebClientResponseException response) {
                return httpFailure(response.getStatusCode().value(),
                        response.getHeaders().getOrEmpty(HttpHeaders.RETRY_AFTER), failure);
            }
            if (cause instanceof ApiException response && response.getCode() > 0) {
                return httpFailure(response.getCode(), retryAfterHeaders(response.getResponseHeaders()), failure);
            }
            if (cause instanceof DecodingException || cause instanceof DecoderException
                    || cause instanceof DataBufferLimitException || cause instanceof ProtocolException) {
                return invalidResponse("Invalid VIPS response.", failure);
            }
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof IOException) {
                return new UpstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Unable to communicate with VIPS.", failure, DEFAULT_RETRY_AFTER);
            }
        }
        return localFailure(failure);
    }

    private static RuntimeException localFailure(Throwable failure) {
        return failure instanceof RuntimeException runtime ? runtime
                : new DigitalFormsException("VIPS client could not execute the request.", failure);
    }

    private static UpstreamServiceException httpFailure(int code, List<String> retryAfter, Throwable cause) {
        HttpStatus status = switch (code) {
            case 408, 504 -> HttpStatus.GATEWAY_TIMEOUT;
            case 429, 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return new UpstreamServiceException(status, "VIPS returned HTTP " + code + ".", cause,
                status == HttpStatus.SERVICE_UNAVAILABLE ? validRetryAfter(retryAfter) : null);
    }

    private static List<String> retryAfterHeaders(Map<String, List<String>> headers) {
        return headers == null ? List.of() : headers.entrySet().stream()
                .filter(entry -> HttpHeaders.RETRY_AFTER.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream()).toList();
    }

    private static String validRetryAfter(List<String> values) {
        if (values.isEmpty()) {
            return DEFAULT_RETRY_AFTER;
        }
        if (values.size() == 1) {
            String value = values.getFirst().trim();
            if (value.matches("[0-9]+")) {
                return value;
            }
            try {
                ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                return value;
            } catch (DateTimeParseException ex) {
                // Do not expose an arbitrary upstream header value to callers.
            }
        }
        log.warn("Ignoring invalid VIPS Retry-After response header; using {} seconds", DEFAULT_RETRY_AFTER);
        return DEFAULT_RETRY_AFTER;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getRetryAfter() {
        return retryAfter;
    }

    public String getSafeDetail() {
        return switch (status) {
            case SERVICE_UNAVAILABLE -> "VIPS is temporarily unavailable.";
            case GATEWAY_TIMEOUT -> "VIPS did not respond in time.";
            default -> "Invalid response from VIPS.";
        };
    }
}
