package jussinet.jalabank.release.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

import jussinet.jalabank.release.repository.TransactionRepository;

/**
 * Every visitor gets their own copy of the demo transactions, kept in their session. Nobody sees what other visitors
 * add or delete, and the copy is gone when the visitor signs out or the session expires.
 * <p>
 * {@link TransactionService} gets a proxy that always works on the current visitor's store.
 */
@Configuration
public class DemoSandboxConfig {

    @Bean
    @SessionScope
    TransactionRepository transactionRepository(DemoDataSeeder demoDataSeeder) {
        TransactionRepository repository = new TransactionRepository();
        demoDataSeeder.addDemoTransactions(repository);
        return repository;
    }
}
