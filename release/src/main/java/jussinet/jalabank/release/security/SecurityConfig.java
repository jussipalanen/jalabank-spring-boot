package jussinet.jalabank.release.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * Who can open what.
 * <p>
 * Signing in has two steps, both handled by {@code LoginController}: choose a customer on {@code /login}, then enter
 * that customer's hourly code on {@code /authenticator}. Only after the second step is the customer signed in, with
 * the {@value CustomerAuthentication#ROLE} role. The sign-in pages, static files, health checks and API docs are
 * public; everything else, the REST API included, needs a signed-in customer.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository)
            throws Exception {
        PathPatternRequestMatcher api = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/authenticator", "/authenticator/codes", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api/health", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/docs", "/docs/**", "/apidocs", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().hasRole(CustomerAuthentication.ROLE))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                // The API uses the session cookie, which is SameSite=Lax: other sites cannot send it with a
                // POST or DELETE, so the API does not need CSRF tokens. The web forms do have them.
                .csrf(csrf -> csrf.ignoringRequestMatchers(api))
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), api)
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"),
                                AnyRequestMatcher.INSTANCE))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        return http.build();
    }
}
