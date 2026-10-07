package jussinet.jalabank.release.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Random;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jussinet.jalabank.release.model.Customer;
import jussinet.jalabank.release.repository.CustomerRepository;

/**
 * Fills the in-memory store with demo data on startup. Everything resets when the application restarts.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    /** How many months of history, before the current one, to generate. */
    static final int MONTHS_OF_HISTORY = 6;

    private final CustomerRepository customerRepository;
    private final TransactionService transactionService;

    public DemoDataSeeder(CustomerRepository customerRepository, TransactionService transactionService) {
        this.customerRepository = customerRepository;
        this.transactionService = transactionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Customer john = customerRepository.save(new Customer(1, "John", "Doe", "john@doe.com"));
        Customer jane = customerRepository.save(new Customer(2, "Jane", "Doe", "jane@doe.com"));

        // A fixed seed keeps the demo data the same on every start
        Random random = new Random(2023);
        LocalDate today = transactionService.today();
        LocalDate start = today.minusMonths(MONTHS_OF_HISTORY).withDayOfMonth(1);

        add(john, start, "2500.00", "Opening deposit");
        add(jane, start, "800.00", "Opening deposit");
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            switch (day.getDayOfMonth()) {
                case 1 -> add(john, day, "3200.00", "Salary");
                case 3 -> add(john, day, "-950.00", "Rent");
                case 15 -> add(john, day, randomAmount(random, -90, -40), "Electricity");
                default -> {
                    // nothing fixed on other days
                }
            }
            if (day.getDayOfWeek().getValue() == 6) {
                add(john, day, randomAmount(random, -140, -35), "Groceries");
            }
            if (random.nextInt(10) == 0) {
                add(john, day, randomAmount(random, -80, -5), "Card payment");
            }

            switch (day.getDayOfMonth()) {
                case 5 -> add(jane, day, "-720.00", "Rent");
                case 25 -> add(jane, day, "2800.00", "Salary");
                default -> {
                    // nothing fixed on other days
                }
            }
            if (day.getDayOfWeek().getValue() == 3) {
                add(jane, day, randomAmount(random, -95, -25), "Groceries");
            }
        }
    }

    private void add(Customer customer, LocalDate date, String amount, String message) {
        transactionService.addTransaction(customer.id(), date, new BigDecimal(amount), message);
    }

    private static String randomAmount(Random random, int min, int max) {
        double value = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
