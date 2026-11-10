package ca.bc.gov.open.digitalformsapi.viirp.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import ca.bc.gov.open.digitalformsapi.viirp.model.ErrorMessage;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import tools.jackson.databind.json.JsonMapper;

/**
 * Establishes the correlation ID used to trace a request in the logs.
 * <p>
 * Uses the optional {@value #HEADER} request header, or a generated UUID when it is absent, and
 * echoes the effective value in the {@value #HEADER} response header. An invalid value, or more
 * than one header, is rejected with 400. Ordered ahead of Spring Security so authentication
 * failures are also traced.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    /** Must match the CorrelationId parameter pattern in VI_IRP_V1.yaml. */
    public static final String PATTERN = "^[A-Za-z0-9._-]{1,64}$";
    public static final String INVALID_MESSAGE = "Invalid " + HEADER
            + " header: expected a single value of 1-64 characters from A-Z, a-z, 0-9, '.', '_' or '-'.";

    private static final String ATTRIBUTE = CorrelationIdFilter.class.getName() + ".ID";
    private static final Pattern VALID = Pattern.compile(PATTERN);
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    private final JsonMapper jsonMapper;

    public CorrelationIdFilter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        // Error dispatches reuse the request's ID so container error responses are traced too.
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = (String) request.getAttribute(ATTRIBUTE);
        boolean valid = true;
        if (correlationId == null) {
            Enumeration<String> headers = request.getHeaders(HEADER);
            List<String> supplied = headers == null ? List.of() : Collections.list(headers);
            if (supplied.size() == 1 && VALID.matcher(supplied.get(0)).matches()) {
                correlationId = supplied.get(0);
            } else {
                valid = supplied.isEmpty();
                correlationId = UUID.randomUUID().toString();
            }
            request.setAttribute(ATTRIBUTE, correlationId);
        }

        MDC.put(DigitalFormsConstants.REQUEST_CORRELATION_ID, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            if (valid) {
                chain.doFilter(request, response);
            } else {
                reject(response);
            }
        } finally {
            MDC.remove(DigitalFormsConstants.REQUEST_CORRELATION_ID);
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        // The supplied value is deliberately not logged; it is untrusted input.
        log.warn("Request rejected (400): invalid {} header", HEADER);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(DigitalFormsConstants.JSON_CONTENT);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), new ErrorMessage(INVALID_MESSAGE));
    }
}
