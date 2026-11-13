package ca.bc.gov.open.digitalformsapi.viirp.exception;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import ca.bc.gov.open.digitalformsapi.viirp.model.ErrorMessage;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import tools.jackson.databind.json.JsonMapper;

/**
 * Authentication failure response handler: adds the RFC 6750 Bearer challenge and writes the API's JSON error body.
 * 
 * @author 237563
 *
 */
@Component
public class DigitalFormsAuthenticationFailureHandler implements AuthenticationEntryPoint, AuthenticationFailureHandler {

	public static final String REALM = "DigitalForms - VI IRP API";
	public static final String UNAUTHORIZED_MESSAGE = "401 - Unauthorized entry, please authenticate";

	private final Logger logger = LoggerFactory.getLogger(DigitalFormsAuthenticationFailureHandler.class);
	private final JsonMapper jsonMapper;
	private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();

	public DigitalFormsAuthenticationFailureHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
		bearerEntryPoint.setRealmName(REALM);
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
		writeError(response, status == HttpServletResponse.SC_UNAUTHORIZED ? UNAUTHORIZED_MESSAGE
				: HttpStatus.valueOf(status).getReasonPhrase());
	}

	/** A presented token could not be checked, for example because DIAM's signing keys could not be downloaded. */
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		if (exception instanceof AuthenticationServiceException) {
			logger.error("API bearer token could not be validated: {}", exception.getMessage(), exception);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			writeError(response, "Internal Server Error");
			return;
		}
		commence(request, response, exception);
	}

	private void writeError(HttpServletResponse response, String message) throws IOException {
		response.setContentType(DigitalFormsConstants.JSON_CONTENT);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), new ErrorMessage(message));
	}

}