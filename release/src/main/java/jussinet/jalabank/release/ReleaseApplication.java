package jussinet.jalabank.release;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Main application. Customers sign in with the authenticator code (see {@code SecurityConfig}), so Spring Boot's
 * default user with a generated password is not needed.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ReleaseApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReleaseApplication.class, args);
	}

	/** The clock for "today"; tests can replace it */
	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}
}
