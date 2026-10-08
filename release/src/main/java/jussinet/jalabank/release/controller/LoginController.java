package jussinet.jalabank.release.controller;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jussinet.jalabank.release.model.Customer;
import jussinet.jalabank.release.repository.CustomerRepository;
import jussinet.jalabank.release.security.AuthenticatorService;
import jussinet.jalabank.release.security.CustomerAuthentication;

/**
 * Signing in: choose a customer, then enter the customer's code from the authenticator.
 * <p>
 * Between the two steps the chosen customer is kept in the session, but nobody is signed in yet. The customer is
 * signed in only after the right code.
 */
@Controller
public class LoginController {

    /** Session attribute: the id of the customer who has started signing in */
    static final String PENDING_CUSTOMER = "jalabank.pendingCustomer";

    /** Session attribute: how many wrong codes have been entered for the pending customer */
    static final String FAILED_ATTEMPTS = "jalabank.failedAttempts";

    /** Wrong codes allowed before the customer has to start over */
    static final int MAX_ATTEMPTS = 5;

    private final CustomerRepository customerRepository;
    private final AuthenticatorService authenticatorService;
    private final SecurityContextRepository securityContextRepository;

    LoginController(CustomerRepository customerRepository, AuthenticatorService authenticatorService,
            SecurityContextRepository securityContextRepository) {
        this.customerRepository = customerRepository;
        this.authenticatorService = authenticatorService;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Step 1: choose a customer
     */
    @GetMapping("/login")
    public String login(Model model, Authentication authentication,
            @SessionAttribute(name = PENDING_CUSTOMER, required = false) Long pendingCustomerId,
            @RequestParam(required = false) String logout) {
        if (isSignedIn(authentication)) {
            return "redirect:/";
        }
        if (logout != null) {
            model.addAttribute("success", "You have signed out.");
        }
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("selectedId", pendingCustomerId);
        return "login";
    }

    @PostMapping("/login")
    public String startLogin(@RequestParam(required = false) Long customerId, HttpSession session,
            RedirectAttributes redirect) {
        Optional<Customer> customer = customerId != null ? customerRepository.findById(customerId) : Optional.empty();
        if (customer.isEmpty()) {
            redirect.addFlashAttribute("warning", "Choose a customer to sign in.");
            return "redirect:/login";
        }
        session.setAttribute(PENDING_CUSTOMER, customer.get().id());
        session.removeAttribute(FAILED_ATTEMPTS);
        return "redirect:/authenticator";
    }

    /**
     * Step 2: enter the code
     */
    @GetMapping("/authenticator")
    public String authenticator(Model model,
            @SessionAttribute(name = PENDING_CUSTOMER, required = false) Long pendingCustomerId,
            @SessionAttribute(name = FAILED_ATTEMPTS, required = false) Integer failedAttempts) {
        Optional<Customer> customer = pendingCustomer(pendingCustomerId);
        if (customer.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("customer", customer.get());
        model.addAttribute("attemptsLeft", MAX_ATTEMPTS - (failedAttempts != null ? failedAttempts : 0));
        return "authenticator";
    }

    @PostMapping("/authenticator")
    public String verify(@RequestParam(required = false) String code, HttpServletRequest request,
            HttpServletResponse response, RedirectAttributes redirect) {
        HttpSession session = request.getSession();
        Optional<Customer> customer = pendingCustomer((Long) session.getAttribute(PENDING_CUSTOMER));
        if (customer.isEmpty()) {
            redirect.addFlashAttribute("warning", "Choose a customer to sign in.");
            return "redirect:/login";
        }

        String digits = code != null ? code.replaceAll("\\s", "") : "";
        if (!authenticatorService.verify(customer.get().id(), digits)) {
            Integer previous = (Integer) session.getAttribute(FAILED_ATTEMPTS);
            int failed = (previous != null ? previous : 0) + 1;
            if (failed >= MAX_ATTEMPTS) {
                session.removeAttribute(PENDING_CUSTOMER);
                session.removeAttribute(FAILED_ATTEMPTS);
                redirect.addFlashAttribute("warning", "Too many wrong codes. Choose the customer and try again.");
                return "redirect:/login";
            }
            session.setAttribute(FAILED_ATTEMPTS, failed);
            redirect.addFlashAttribute("warning", "Wrong code. Check the code for the current hour and try again.");
            return "redirect:/authenticator";
        }

        session.removeAttribute(PENDING_CUSTOMER);
        session.removeAttribute(FAILED_ATTEMPTS);
        // A new session id on sign-in, so an id known before signing in is no use afterwards
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(CustomerAuthentication.of(customer.get()));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        redirect.addFlashAttribute("success", "Welcome, " + customer.get().firstname() + "!");
        return "redirect:/";
    }

    /**
     * The authenticator's code list, opened in a pop-up window from the code page
     */
    @GetMapping("/authenticator/codes")
    public String codes(Model model,
            @SessionAttribute(name = PENDING_CUSTOMER, required = false) Long pendingCustomerId) {
        pendingCustomer(pendingCustomerId).ifPresent(customer -> {
            model.addAttribute("customer", customer);
            model.addAttribute("codes", authenticatorService.codes(customer.id()));
            model.addAttribute("minutesLeft", authenticatorService.timeLeft().toMinutes() + 1);
            model.addAttribute("zone", authenticatorService.zone());
        });
        return "authenticator-codes";
    }

    private Optional<Customer> pendingCustomer(Long customerId) {
        return customerId != null ? customerRepository.findById(customerId) : Optional.empty();
    }

    private static boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
