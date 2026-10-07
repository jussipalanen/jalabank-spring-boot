package jussinet.jalabank.release.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Repository;

import jussinet.jalabank.release.model.Transaction;

/**
 * In-memory transaction store. Contents live only as long as the application runs.
 */
@Repository
public class TransactionRepository {

    private final List<Transaction> transactions = new ArrayList<>();

    public synchronized Transaction save(Transaction transaction) {
        transactions.add(transaction);
        return transaction;
    }

    /**
     * All transactions in booking order: by date, and by insertion order within the same date.
     */
    public synchronized List<Transaction> findAllChronological() {
        List<Transaction> copy = new ArrayList<>(transactions);
        copy.sort(Comparator.comparing(Transaction::date));
        return copy;
    }

    public synchronized int count() {
        return transactions.size();
    }
}
