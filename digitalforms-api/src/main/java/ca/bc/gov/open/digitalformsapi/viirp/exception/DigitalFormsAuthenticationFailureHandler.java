package ca.bc.gov.open.digitalformsapi.viirp.exception;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

/**
 * Authentication failure response handler
 * 
 * @author 237563
 *
 */
@Component
public class DigitalFormsAuthenticationFailureHandler extends BasicAuthenticationEntryPoint  {
	
	private final Logger logger = LoggerFactory.getLogger(DigitalFormsAuthenticationFailureHandler.class);

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		
		String errorMessage = "401 - Unauthorized entry, please authenticate";
		JSONObject json = new JSONObject();
		json.put("status_message", errorMessage);
		
		logger.debug("API basic authentication failed");
		response.setContentType(DigitalFormsConstants.JSON_CONTENT);
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().print(json.toString());
	}
	
	@Override
    public void afterPropertiesSet() {
        setRealmName("DigitalForms - VI IRP API");
        super.afterPropertiesSet();
    }

}
