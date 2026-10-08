package jussinet.jalabank.release.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jussinet.jalabank.release.model.Transaction;

/**
 * In-memory transaction store. Each visitor has their own (see {@code DemoSandboxConfig}), which lives only as long
 * as the visitor's session.
 */
public class TransactionRepository {

    private final List<Transaction> transactions = new ArrayList<>();

    public synchronized Transaction save(Transaction transaction) {
        transactions.add(transaction);
        return transaction;
    }

    public synchronized Optional<Transaction> findById(UUID id) {
        return transactions.stream().filter(t -> t.id().equals(id)).findFirst();
    }

    /**
     * All transactions in booking order: by date, and by insertion order within the same date.
     */
    public synchronized List<Transaction> findAllChronological() {
        List<Transaction> copy = new ArrayList<>(transactions);
        copy.sort(Comparator.comparing(Transaction::date));
        return copy;
    }

    /**
     * Deletes the transactions with the given ids.
     *
     * @return the ids that were deleted
     */
    public synchronized Set<UUID> deleteAllById(Collection<UUID> ids) {
        Set<UUID> wanted = new HashSet<>(ids);
        Set<UUID> deleted = new HashSet<>();
        transactions.removeIf(t -> {
            if (wanted.contains(t.id())) {
                deleted.add(t.id());
                return true;
            }
            return false;
        });
        return deleted;
    }

    public synchronized int count() {
        return transactions.size();
    }
}
