package jussinet.jalabank.release.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jussinet.jalabank.release.model.DeleteResult;
import jussinet.jalabank.release.model.MonthSummary;
import jussinet.jalabank.release.model.PageResult;
import jussinet.jalabank.release.model.SortDirection;
import jussinet.jalabank.release.model.StatementRow;
import jussinet.jalabank.release.model.Transaction;
import jussinet.jalabank.release.model.TransactionApiData;
import jussinet.jalabank.release.repository.TransactionRepository;

/**
 * Balance calculations over the in-memory transactions.
 *
 * <p>
 * Every balance belongs to one customer: it only counts that customer's transactions. The cumulative balance after a
 * transaction is the sum of that customer's transactions up to and including it, in booking order (by date, then by
 * the order they were added in).
 */
@Service
public class TransactionService {

    /** How many transactions one visitor can have, so a public demo cannot grow without bound. */
    public static final int MAX_TRANSACTIONS = 1_000;

    private static final Map<String, Comparator<Transaction>> API_SORTS = Map.of(
            "id", Comparator.comparing(Transaction::id),
            "amount", Comparator.comparing(Transaction::amount),
            "date", Comparator.comparing(Transaction::date));

    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public TransactionService(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public Transaction addTransaction(long customerId, LocalDate date, BigDecimal amount, String message) {
        if (transactionRepository.count() >= MAX_TRANSACTIONS) {
            throw new IllegalStateException("Your demo is full (" + MAX_TRANSACTIONS + " transactions). Delete some, "
                    + "or sign out and in again to start over.");
        }
        return transactionRepository.save(new Transaction(UUID.randomUUID(), customerId, date, amount, message));
    }

    public Optional<Transaction> findById(UUID id) {
        return transactionRepository.findById(id);
    }

    /** The transaction, if it exists and belongs to the customer */
    public Optional<Transaction> findById(long customerId, UUID id) {
        return findById(id).filter(t -> t.customerId() == customerId);
    }

    /**
     * Deletes the customer's transactions with the given ids. Ids of other customers' transactions count as not
     * found, so a customer can only delete their own.
     */
    public DeleteResult delete(long customerId, Collection<UUID> ids) {
        Set<UUID> own = ids.stream()
                .filter(id -> isOwnedBy(id, customerId))
                .collect(Collectors.toSet());
        Set<UUID> deleted = transactionRepository.deleteAllById(own);
        List<UUID> notFound = ids.stream().distinct().filter(id -> !deleted.contains(id)).toList();
        return new DeleteResult(deleted.size(), notFound);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * The customer's balance today. Transactions dated in the future are not counted yet.
     */
    public BigDecimal currentBalance(long customerId) {
        return balanceOn(customerId, today());
    }

    /**
     * The customer's balance at the end of the given day.
     */
    public BigDecimal balanceOn(long customerId, LocalDate date) {
        return sum(ledger(customerId).stream().filter(t -> !t.date().isAfter(date)).toList());
    }

    /**
     * The customer's transactions dated after today, in booking order.
     */
    public List<Transaction> scheduled(long customerId) {
        LocalDate today = today();
        return ledger(customerId).stream().filter(t -> t.date().isAfter(today)).toList();
    }

    public MonthSummary monthSummary(long customerId, YearMonth month) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        int count = 0;
        for (Transaction t : ledger(customerId)) {
            if (YearMonth.from(t.date()).equals(month)) {
                count++;
                if (t.amount().signum() >= 0) {
                    income = income.add(t.amount());
                } else {
                    expenses = expenses.add(t.amount());
                }
            }
        }
        return new MonthSummary(income, expenses, balanceOn(customerId, month.atEndOfMonth()), count);
    }

    /**
     * The customer's transactions of the given month, each with the cumulative balance after it.
     *
     * @param page zero-based page number; a page past the end shows the last page
     */
    public PageResult<StatementRow> statement(long customerId, YearMonth month, int page, int size) {
        List<StatementRow> rows = statementRows(customerId).stream()
                .filter(row -> YearMonth.from(row.date()).equals(month))
                .toList();
        return PageResult.ofClamped(rows, page, size);
    }

    /**
     * The customer's latest transactions up to today, newest first, each with the cumulative balance after it.
     */
    public List<StatementRow> recent(long customerId, int limit) {
        LocalDate today = today();
        List<StatementRow> rows = new ArrayList<>(statementRows(customerId).stream()
                .filter(row -> !row.date().isAfter(today))
                .toList());
        return rows.reversed().stream().limit(limit).toList();
    }

    /**
     * Transactions for the REST API.
     *
     * @param customerId only this customer's transactions, or all when {@code null}
     * @param page zero-based page number
     * @param sortBy one of {@code id}, {@code amount} or {@code date}
     * @throws IllegalArgumentException if {@code sortBy} is not supported
     */
    public PageResult<TransactionApiData> apiTransactions(Long customerId, int page, int size, String sortBy,
            SortDirection direction) {
        Comparator<Transaction> comparator = API_SORTS.get(sortBy);
        if (comparator == null) {
            throw new IllegalArgumentException("sortBy must be one of " + API_SORTS.keySet());
        }
        if (direction == SortDirection.DESC) {
            comparator = comparator.reversed();
        }
        List<TransactionApiData> sorted = transactionRepository.findAllChronological().stream()
                .filter(t -> customerId == null || t.customerId() == customerId)
                .sorted(comparator)
                .map(TransactionApiData::from)
                .toList();
        return PageResult.of(sorted, page, size);
    }

    public int count() {
        return transactionRepository.count();
    }

    public int count(long customerId) {
        return ledger(customerId).size();
    }

    private boolean isOwnedBy(UUID id, long customerId) {
        return findById(customerId, id).isPresent();
    }

    /** The customer's transactions in booking order */
    private List<Transaction> ledger(long customerId) {
        return transactionRepository.findAllChronological().stream()
                .filter(t -> t.customerId() == customerId)
                .toList();
    }

    /** The customer's transactions in booking order, each with the cumulative balance after it */
    private List<StatementRow> statementRows(long customerId) {
        List<StatementRow> rows = new ArrayList<>();
        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction t : ledger(customerId)) {
            balance = balance.add(t.amount());
            rows.add(new StatementRow(t.id(), t.date(), t.amount(), t.message(), balance));
        }
        return rows;
    }

    private static BigDecimal sum(List<Transaction> transactions) {
        return transactions.stream().map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
