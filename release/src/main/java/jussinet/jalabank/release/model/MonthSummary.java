package jussinet.jalabank.release.model;

import java.math.BigDecimal;

/**
 * Totals of one customer's transactions in one month.
 *
 * @param endBalance the balance after the last transaction on or before the last day of the month
 */
public record MonthSummary(BigDecimal income, BigDecimal expenses, BigDecimal endBalance, int count) {
}
