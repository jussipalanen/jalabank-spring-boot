package jussinet.boobank.release.model;

/**
 * A bank customer. Passwords and other credentials are intentionally not part of the demo.
 */
public record Customer(long id, String firstname, String lastname, String email) {

    public String fullName() {
        return firstname + " " + lastname;
    }
}
