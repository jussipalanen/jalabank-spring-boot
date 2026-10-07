package jussinet.jalabank.release.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jussinet.jalabank.release.model.PageResult;
import jussinet.jalabank.release.model.SortDirection;
import jussinet.jalabank.release.model.StatementRow;
import jussinet.jalabank.release.model.Transaction;
import jussinet.jalabank.release.model.TransactionApiData;
import jussinet.jalabank.release.repository.TransactionRepository;

/**
 * Balance calculations over the in-memory transactions.
 */
@Service
public class TransactionService {

    /** Keeps a publicly hosted demo from growing without bound. */
    public static final int MAX_TRANSACTIONS = 10_000;

    private static final Map<String, Comparator<Transaction>> API_SORTS = Map.of(
            "id", Comparator.comparing(Transaction::id),
            "amount", Comparator.comparing(Transaction::amount),
            "date", Comparator.comparing(Transaction::date));

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction addTransaction(long customerId, LocalDate date, BigDecimal amount, String message) {
        if (transactionRepository.count() >= MAX_TRANSACTIONS) {
            throw new IllegalStateException("The demo is full. Restart the application to reset the demo data.");
        }
        return transactionRepository.save(new Transaction(UUID.randomUUID(), customerId, date, amount, message));
    }

    /**
     * The balance after every transaction, including future-dated ones.
     */
    public BigDecimal currentBalance() {
        return sum(transactionRepository.findAllChronological());
    }

    /**
     * The balance after the last transaction of the given month.
     */
    public BigDecimal balanceAtEndOf(YearMonth month) {
        LocalDate end = month.atEndOfMonth();
        return sum(transactionRepository.findAllChronological().stream()
                .filter(t -> !t.date().isAfter(end))
                .toList());
    }

    /**
     * The transactions of the given month, each with the cumulative balance after it.
     *
     * @param page zero-based page number
     */
    public PageResult<StatementRow> statement(YearMonth month, int page, int size) {
        List<StatementRow> rows = new ArrayList<>();
        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction t : transactionRepository.findAllChronological()) {
            balance = balance.add(t.amount());
            if (YearMonth.from(t.date()).equals(month)) {
                rows.add(new StatementRow(t.id(), t.date(), t.amount(), t.message(), balance));
            }
        }
        return PageResult.of(rows, page, size);
    }

    /**
     * Transactions for the REST API.
     *
     * @param page zero-based page number
     * @param sortBy one of {@code id}, {@code amount} or {@code date}
     * @throws IllegalArgumentException if {@code sortBy} is not supported
     */
    public PageResult<TransactionApiData> apiTransactions(int page, int size, String sortBy, SortDirection direction) {
        Comparator<Transaction> comparator = API_SORTS.get(sortBy);
        if (comparator == null) {
            throw new IllegalArgumentException("sortBy must be one of " + API_SORTS.keySet());
        }
        if (direction == SortDirection.DESC) {
            comparator = comparator.reversed();
        }
        List<TransactionApiData> sorted = transactionRepository.findAllChronological().stream()
                .sorted(comparator)
                .map(TransactionApiData::from)
                .toList();
        return PageResult.of(sorted, page, size);
    }

    public int count() {
        return transactionRepository.count();
    }

    private static BigDecimal sum(List<Transaction> transactions) {
        return transactions.stream().map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
