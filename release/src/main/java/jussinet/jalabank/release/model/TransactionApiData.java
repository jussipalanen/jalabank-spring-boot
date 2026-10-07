package jussinet.jalabank.release.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A transaction as returned by the REST API.
 */
@JsonPropertyOrder({ "id", "customerId", "date", "amount", "message" })
@Schema(name = "Transaction", description = "A booked transaction")
public record TransactionApiData(
        @Schema(example = "3f1c2b9e-8d4a-4c1e-9b7f-2a6d5e4c3b21") UUID id,
        @Schema(example = "1") long customerId,
        @Schema(example = "2026-10-01") LocalDate date,
        @Schema(description = "Positive for deposits, negative for withdrawals", example = "-54.20") BigDecimal amount,
        @Schema(example = "Groceries") String message) {

    public static TransactionApiData from(Transaction transaction) {
        return new TransactionApiData(transaction.id(), transaction.customerId(), transaction.date(),
                transaction.amount(), transaction.message());
    }
}
