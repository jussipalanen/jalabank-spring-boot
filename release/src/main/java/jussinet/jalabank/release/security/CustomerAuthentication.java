package jussinet.jalabank.release.security;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import jussinet.jalabank.release.model.Customer;

/**
 * The signed-in state of a customer who has entered the right authenticator code. The principal is the
 * {@link Customer}, so controllers can take it with {@code @AuthenticationPrincipal Customer customer}.
 */
public final class CustomerAuthentication {

    /** The role every fully signed-in customer has */
    public static final String ROLE = "CUSTOMER";

    private CustomerAuthentication() {
    }

    public static Authentication of(Customer customer) {
        return UsernamePasswordAuthenticationToken.authenticated(customer, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + ROLE)));
    }
}
