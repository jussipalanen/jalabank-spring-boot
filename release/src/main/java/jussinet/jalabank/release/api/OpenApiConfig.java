package jussinet.jalabank.release.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * The description shown at the top of the API docs (/docs).
 */
@Configuration
public class OpenApiConfig {

    /** The security scheme of the endpoints that need a signed-in customer */
    static final String SESSION_COOKIE = "session";

    @Bean
    OpenAPI jalabankOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Jalabank API")
                .version("v1")
                .description("""
                        REST API of the Jalabank demo bank.

                        All data is demo data kept in memory: changes made here reset when the application \
                        restarts. Amounts are in euros; deposits are positive and withdrawals negative.

                        The /api/v1 endpoints need a signed-in customer and work on that customer's own data. \
                        Sign in on the [sign-in page](/login) in this browser first; "Try it out" then sends \
                        the session cookie along. Without it, the API answers 401.""")
                .license(new License().name("Demo application")))
                .components(new Components().addSecuritySchemes(SESSION_COOKIE, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                        .description("The session cookie of a signed-in customer")));
    }
}
