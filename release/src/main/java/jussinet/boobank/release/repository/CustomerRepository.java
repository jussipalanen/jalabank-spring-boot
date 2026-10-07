package jussinet.boobank.release.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.stereotype.Repository;

import jussinet.boobank.release.model.Customer;

/**
 * In-memory customer store. Contents live only as long as the application runs.
 */
@Repository
public class CustomerRepository {

    private final Map<Long, Customer> customers = new ConcurrentSkipListMap<>();

    public List<Customer> findAll() {
        return List.copyOf(customers.values());
    }

    public Optional<Customer> findById(long id) {
        return Optional.ofNullable(customers.get(id));
    }

    public Customer save(Customer customer) {
        customers.put(customer.id(), customer);
        return customer;
    }
}
