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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

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
	@Autowired
	private ConfigProperties properties;

	@Autowired
	private DigitalFormsAuthenticationFailureHandler authenticationFailureHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.sessionManagement(
				httpSecuritySessionManagementConfigurer -> {
					httpSecuritySessionManagementConfigurer.sessionCreationPolicy(
							SessionCreationPolicy.STATELESS);
				});

		http.authorizeHttpRequests(requests -> requests
				.requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
				.requestMatchers(new AntPathRequestMatcher("/swagger-resources/**")).permitAll()
				.requestMatchers(new AntPathRequestMatcher("/swagger-ui/index.html")).permitAll()
				.requestMatchers(new AntPathRequestMatcher("/swagger-ui.html")).permitAll()
				.requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
				.requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
				.anyRequest().authenticated()).httpBasic(withDefaults());

		// Authentication failure error response handler
		http.httpBasic(
				httpSecurityHttpBasicConfigurer -> {
					httpSecurityHttpBasicConfigurer.authenticationEntryPoint(
							authenticationFailureHandler);
				});

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
