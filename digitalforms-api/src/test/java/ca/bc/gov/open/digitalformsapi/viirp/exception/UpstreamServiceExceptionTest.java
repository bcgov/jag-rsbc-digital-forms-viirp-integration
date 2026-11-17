package ca.bc.gov.open.digitalformsapi.viirp.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.codec.EncodingException;
import org.springframework.http.HttpStatus;

import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiException;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;

class UpstreamServiceExceptionTest {

    @ParameterizedTest
    @MethodSource("timeouts")
    void recognizesTimeoutsEvenInsideClientAndRetryWrappers(Throwable timeout) {
        var failure = new DigitalFormsException("Retry budget exhausted", new ApiException(timeout));
        var result = assertInstanceOf(UpstreamServiceException.class, UpstreamServiceException.from(failure));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, result.getStatus());
        assertSame(failure, result.getCause());
    }

    static Stream<Throwable> timeouts() {
        return Stream.of(new SocketTimeoutException(), new TimeoutException(),
                new ConnectTimeoutException(), ReadTimeoutException.INSTANCE, WriteTimeoutException.INSTANCE);
    }

    @Test
    void unrelatedProgrammingAndRequestEncodingFailuresAreNotUpstreamFailures() {
        for (var failure : List.of(new IllegalArgumentException("Invalid local URL"),
                new NullPointerException("Local bug"),
                new EncodingException("Cannot serialize local request", new IOException("local failure")))) {
            assertSame(failure, UpstreamServiceException.from(failure));
        }
        var local = new ApiException("Missing required parameter");
        assertFalse(UpstreamServiceException.from(local) instanceof UpstreamServiceException);
    }

    @Test
    void duplicateRetryAfterValuesUseTheSafeDefault() {
        var failure = new ApiException(503, "Unavailable", Map.of("retry-after", List.of("10", "20")), null);
        var result = assertInstanceOf(UpstreamServiceException.class, UpstreamServiceException.from(failure));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatus());
        assertEquals("30", result.getRetryAfter());
    }
}
