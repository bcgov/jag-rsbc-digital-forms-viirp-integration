package ca.bc.gov.open.digitalformsapi.viirp.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsAuthenticationFailureHandler;

/**
 * 
 * This class enforces Basic Auth on all API operations.  
 * 
 * It is assumed all Actuator calls are prefixed with /actuator/*  (No security)
 * 
 * Excludes all Swagger UI related endpoints (No security)
 * 
 * @author 237563
 *
 */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {
	
	private static final String[] AUTH_WHITELIST = {

            // Non protected Swagger UI and Actuator endpoints.
			"/swagger-ui/**",
            "/swagger-resources/**",
            "/swagger-ui/index.html",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/**"
    };
	
	@Autowired
	private ConfigProperties properties;
	
	@Autowired
	private DigitalFormsAuthenticationFailureHandler authenticationFailureHandler;
	
	@Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
		// Stateless session validates basic auth credentials for each request
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		http.csrf().disable().authorizeRequests()
			.antMatchers(AUTH_WHITELIST).permitAll()
			.anyRequest().authenticated()
			.and()
			.httpBasic();
		
		// Authentication failure error response handler
		http.exceptionHandling().authenticationEntryPoint(authenticationFailureHandler);
		
		return http.build();
	}
	
	@Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.builder()
            .username(properties.getDigitalFormsBasicAuthUser())
            .password(passwordEncoder().encode(properties.getDigitalFormsBasicAuthPassword()))
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
}
