package jussinet.jalabank.release.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form backing object for the "Add transaction" page. The transaction is always for the signed-in customer.
 */
public class TransactionForm {

    public enum TransferMethod {
        DEPOSIT, WITHDRAW
    }

    @NotNull(message = "The amount cannot be empty")
    @DecimalMin(value = "0.01", message = "The amount must be at least 0.01")
    @DecimalMax(value = "9999999", message = "The amount can be at most 9 999 999")
    @Digits(integer = 7, fraction = 2, message = "Use at most two decimals")
    private BigDecimal amount;

    @NotNull
    private TransferMethod transferMethod = TransferMethod.DEPOSIT;

    @NotNull(message = "The date cannot be empty")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date = LocalDate.now();

    @Size(max = 255, message = "The message can be at most 255 characters")
    private String message;

    /**
     * The amount with its sign: negative for withdrawals.
     */
    public BigDecimal signedAmount() {
        return transferMethod == TransferMethod.WITHDRAW ? amount.negate() : amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransferMethod getTransferMethod() {
        return transferMethod;
    }

    public void setTransferMethod(TransferMethod transferMethod) {
        this.transferMethod = transferMethod;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
