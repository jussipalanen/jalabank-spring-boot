package jussinet.jalabank.release.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A transaction as returned by the REST API.
 */
@JsonPropertyOrder({ "id", "amount", "date" })
public record TransactionApiData(UUID id, BigDecimal amount, LocalDate date) {

    public static TransactionApiData from(Transaction transaction) {
        return new TransactionApiData(transaction.id(), transaction.amount(), transaction.date());
    }
}
