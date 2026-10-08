package jussinet.jalabank.release.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A new transaction sent to the REST API.
 */
@Schema(name = "NewTransaction", description = "A new transaction")
public record TransactionRequest(
        @Schema(description = "Optional; must be the signed-in customer's id", example = "1") Long customerId,
        @Schema(description = "Defaults to today", example = "2026-10-07") LocalDate date,
        @Schema(description = "Positive for a deposit, negative for a withdrawal", example = "-12.50")
        @NotNull @DecimalMin("-9999999") @DecimalMax("9999999") @Digits(integer = 7, fraction = 2) BigDecimal amount,
        @Schema(example = "Coffee") @Size(max = 255) String message) {

    @AssertTrue(message = "amount must not be zero")
    @Schema(hidden = true)
    public boolean isAmountNonZero() {
        return amount == null || amount.signum() != 0;
    }
}
