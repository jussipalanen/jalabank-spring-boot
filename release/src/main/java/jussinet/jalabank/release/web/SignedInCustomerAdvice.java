package jussinet.jalabank.release.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jussinet.jalabank.release.model.Customer;

/**
 * Makes the signed-in customer available to every page as {@code signedIn}, for the navigation bar.
 * It is {@code null} when nobody is signed in.
 */
@ControllerAdvice
public class SignedInCustomerAdvice {

    @ModelAttribute("signedIn")
    Customer signedIn(@AuthenticationPrincipal Customer customer) {
        return customer;
    }
}
