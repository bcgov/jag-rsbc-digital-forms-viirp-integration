package ca.bc.gov.open.digitalformsapi.viirp.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ca.bc.gov.open.digitalformsapi.viirp.interceptor.MdcInterceptor;

@Component
public class WebMvcConfiguration implements WebMvcConfigurer {
	
	@Override
    public void addInterceptors(InterceptorRegistry registry) {
		// adds MDC Interceptor to capture and release MDC correlation Id
        registry.addInterceptor(new MdcInterceptor());
    }
}
