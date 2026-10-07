package jussinet.jalabank.release;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Main application
 */
@SpringBootApplication
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
