package jussinet.jalabank.release.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * The description shown at the top of the API docs (/docs).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI jalabankOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Jalabank API")
                .version("v1")
                .description("""
                        REST API of the Jalabank demo bank.

                        All data is demo data kept in memory: changes made here reset when the application \
                        restarts. Amounts are in euros; deposits are positive and withdrawals negative.""")
                .license(new License().name("Demo application")));
    }
}
