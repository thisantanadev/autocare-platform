package com.autocare.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Authentication is entirely JWT-based: {@code JwtAuthenticationFilter} is the only
 * authentication mechanism, and the filter chain enables neither HTTP Basic nor form
 * login. Nothing in the application uses {@code UserDetailsService} or
 * {@code AuthenticationManager}, so the default user autoconfiguration only
 * contributed an unreachable in-memory account whose generated password was printed
 * on every startup. Excluding it removes that misleading log line without touching
 * the security chain.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
