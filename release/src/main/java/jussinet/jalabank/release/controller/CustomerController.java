package jussinet.jalabank.release.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jussinet.jalabank.release.model.Customer;
import jussinet.jalabank.release.repository.CustomerRepository;

/*
 * Customer API
 */
@RestController
@RequestMapping("/api/v1")
public class CustomerController {

    private final CustomerRepository customerRepository;

    CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Get all of the customers
     */
    @GetMapping("/customers")
    List<Customer> all() {
        return customerRepository.findAll();
    }

    /**
     * Get a single customer by id
     */
    @GetMapping("/customers/{id}")
    Customer get(@PathVariable long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
}
