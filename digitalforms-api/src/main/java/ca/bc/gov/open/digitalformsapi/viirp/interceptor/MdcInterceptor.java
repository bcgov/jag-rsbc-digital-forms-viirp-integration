package ca.bc.gov.open.digitalformsapi.viirp.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

/**
 * MDC Interceptor
 * 
 *  Captures and releases correlationId from requests for MDC. 
 * 
 * @author 176899
 *
 */
public class MdcInterceptor implements HandlerInterceptor {
	
	private final Logger logger = LoggerFactory.getLogger(MdcInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		@SuppressWarnings("unchecked")
		final Map<String, String> pathVariables = (Map<String, String>) request
				.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		
		String correlationId = "undefined";
		if (null != pathVariables) {
			correlationId = pathVariables.get(DigitalFormsConstants.REQUEST_CORRELATION_ID);
		} 
		MDC.put(DigitalFormsConstants.REQUEST_CORRELATION_ID, correlationId);

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		
		MDC.remove(DigitalFormsConstants.REQUEST_CORRELATION_ID);
	}
}
