package jussinet.jalabank.release.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A transaction as shown on the balances page, together with the cumulative balance after it.
 */
public record StatementRow(UUID id, LocalDate date, BigDecimal amount, String message, BigDecimal cumulativeBalance) {
}
