package jussinet.jalabank.release.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single booked transaction. Positive amounts are deposits, negative amounts are withdrawals.
 */
public record Transaction(UUID id, long customerId, LocalDate date, BigDecimal amount, String message) {
}
