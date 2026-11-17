package ca.bc.gov.open.digitalformsapi.viirp.exception;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * Security failure response handler: preserves RFC 6750 Bearer challenges and writes RFC 9457 Problem Details.
 * 
 * @author 237563
 *
 */
@Component
public class DigitalFormsAuthenticationFailureHandler
		implements AuthenticationEntryPoint, AuthenticationFailureHandler, AccessDeniedHandler {

	public static final String REALM = "DigitalForms - VI IRP API";
	public static final String UNAUTHORIZED_MESSAGE = "401 - Unauthorized entry, please authenticate";

	private final Logger logger = LoggerFactory.getLogger(DigitalFormsAuthenticationFailureHandler.class);
	private final JsonMapper jsonMapper;
	private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
	private final BearerTokenAccessDeniedHandler bearerAccessDeniedHandler = new BearerTokenAccessDeniedHandler();
	private final AccessDeniedHandler csrfAccessDeniedHandler = new AccessDeniedHandlerImpl();

	public DigitalFormsAuthenticationFailureHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
		bearerEntryPoint.setRealmName(REALM);
		bearerAccessDeniedHandler.setRealmName(REALM);
	}

	/** Missing, malformed or rejected bearer token. */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		if (authException instanceof OAuth2AuthenticationException rejected) {
			// The description names the failed check (for example "The azp claim is not valid"), never the token.
			logger.warn("API bearer token rejected: {}", rejected.getError().getDescription());
		} else {
			logger.debug("API request without a bearer token: {}", authException.getMessage());
		}
		bearerEntryPoint.commence(request, response, authException);
		int status = response.getStatus();
		writeError(request, response, status == HttpServletResponse.SC_UNAUTHORIZED ? UNAUTHORIZED_MESSAGE
				: HttpStatus.valueOf(status).getReasonPhrase());
	}

	/** A presented token could not be checked, for example because DIAM's signing keys could not be downloaded. */
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		if (exception instanceof AuthenticationServiceException) {
			logger.error("API bearer token could not be validated: {}", exception.getMessage(), exception);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			writeError(request, response, "Internal Server Error");
			return;
		}
		commence(request, response, exception);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
			throws IOException, ServletException {
		if (exception instanceof CsrfException) {
			// Preserve the existing non-bearer CSRF rejection and container error-dispatch authentication.
			csrfAccessDeniedHandler.handle(request, response, exception);
			return;
		}
		logger.warn("API bearer access denied");
		bearerAccessDeniedHandler.handle(request, response, exception);
		writeError(request, response, "Forbidden");
	}

	private void writeError(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
		ApiProblemDetails.write(jsonMapper, response,
				ApiProblemDetails.create(request, HttpStatus.valueOf(response.getStatus()), message));
	}

}