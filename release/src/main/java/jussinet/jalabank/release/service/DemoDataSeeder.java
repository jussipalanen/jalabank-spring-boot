package jussinet.jalabank.release.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jussinet.jalabank.release.model.Customer;
import jussinet.jalabank.release.model.Transaction;
import jussinet.jalabank.release.repository.CustomerRepository;
import jussinet.jalabank.release.repository.TransactionRepository;

/**
 * The demo data. The customers are created on startup and shared by everyone; the transactions are added to each
 * visitor's own store (see {@link DemoSandboxConfig}) when the visitor first needs them.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    /** How many months of history, before the current one, to generate. */
    static final int MONTHS_OF_HISTORY = 6;

    static final Customer JOHN = new Customer(1, "John", "Doe", "john@doe.com");
    static final Customer JANE = new Customer(2, "Jane", "Doe", "jane@doe.com");

    private final CustomerRepository customerRepository;
    private final Clock clock;

    public DemoDataSeeder(CustomerRepository customerRepository, Clock clock) {
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        customerRepository.save(JOHN);
        customerRepository.save(JANE);
    }

    /**
     * Adds about six months of demo transactions for both customers, up to today.
     */
    public void addDemoTransactions(TransactionRepository repository) {
        // A fixed seed gives every visitor the same demo data
        Random random = new Random(2023);
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusMonths(MONTHS_OF_HISTORY).withDayOfMonth(1);

        add(repository, JOHN, start, "2500.00", "Opening deposit");
        add(repository, JANE, start, "800.00", "Opening deposit");
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            switch (day.getDayOfMonth()) {
                case 1 -> add(repository, JOHN, day, "3200.00", "Salary");
                case 3 -> add(repository, JOHN, day, "-950.00", "Rent");
                case 15 -> add(repository, JOHN, day, randomAmount(random, -90, -40), "Electricity");
                default -> {
                    // nothing fixed on other days
                }
            }
            if (day.getDayOfWeek().getValue() == 6) {
                add(repository, JOHN, day, randomAmount(random, -140, -35), "Groceries");
            }
            if (random.nextInt(10) == 0) {
                add(repository, JOHN, day, randomAmount(random, -80, -5), "Card payment");
            }

            switch (day.getDayOfMonth()) {
                case 5 -> add(repository, JANE, day, "-720.00", "Rent");
                case 25 -> add(repository, JANE, day, "2800.00", "Salary");
                default -> {
                    // nothing fixed on other days
                }
            }
            if (day.getDayOfWeek().getValue() == 3) {
                add(repository, JANE, day, randomAmount(random, -95, -25), "Groceries");
            }
        }
    }

    private static void add(TransactionRepository repository, Customer customer, LocalDate date, String amount,
            String message) {
        repository.save(new Transaction(UUID.randomUUID(), customer.id(), date, new BigDecimal(amount), message));
    }

    private static String randomAmount(Random random, int min, int max) {
        double value = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
