package ca.bc.gov.open.digitalformsapi.viirp.exception;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import ca.bc.gov.open.digitalformsapi.viirp.model.ErrorMessage;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import tools.jackson.databind.json.JsonMapper;

/**
 * Authentication failure response handler
 * 
 * @author 237563
 *
 */
@Component
public class DigitalFormsAuthenticationFailureHandler extends BasicAuthenticationEntryPoint  {
	
	private final Logger logger = LoggerFactory.getLogger(DigitalFormsAuthenticationFailureHandler.class);
	private final JsonMapper jsonMapper;

	public DigitalFormsAuthenticationFailureHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		
		String errorMessage = "401 - Unauthorized entry, please authenticate";
		
		logger.debug("API basic authentication failed");
		response.setContentType(DigitalFormsConstants.JSON_CONTENT);
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setHeader("WWW-Authenticate", "Basic realm=\"" + getRealmName() + "\"");
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), new ErrorMessage(errorMessage));
	}
	
	@Override
    public void afterPropertiesSet() {
        setRealmName("DigitalForms - VI IRP API");
        super.afterPropertiesSet();
    }

}
