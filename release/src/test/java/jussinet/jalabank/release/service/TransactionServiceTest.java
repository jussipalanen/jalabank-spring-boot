package jussinet.jalabank.release.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jussinet.jalabank.release.model.PageResult;
import jussinet.jalabank.release.model.SortDirection;
import jussinet.jalabank.release.model.StatementRow;
import jussinet.jalabank.release.model.TransactionApiData;
import jussinet.jalabank.release.repository.TransactionRepository;

class TransactionServiceTest {

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(new TransactionRepository());
        // Added out of date order on purpose
        add("2026-02-10", "-30.50", "Groceries");
        add("2026-01-01", "1000.00", "Opening");
        add("2026-02-01", "200.00", "Salary");
        add("2026-03-05", "-0.10", "Fee");
    }

    @Test
    void currentBalanceSumsAllTransactionsExactly() {
        assertThat(service.currentBalance()).isEqualByComparingTo("1169.40");
    }

    @Test
    void statementShowsOnlyTheMonthWithRunningBalance() {
        PageResult<StatementRow> page = service.statement(YearMonth.of(2026, 2), 0, 10);

        assertThat(page.content()).extracting(StatementRow::message).containsExactly("Salary", "Groceries");
        assertThat(page.content()).extracting(StatementRow::cumulativeBalance)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1200.00"), new BigDecimal("1169.50"));
    }

    @Test
    void statementPaginates() {
        PageResult<StatementRow> page = service.statement(YearMonth.of(2026, 2), 1, 1);

        assertThat(page.content()).extracting(StatementRow::message).containsExactly("Groceries");
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.pageNumbers()).containsExactly(1, 2);
    }

    @Test
    void balanceAtEndOfMonthIncludesEarlierMonths() {
        assertThat(service.balanceAtEndOf(YearMonth.of(2026, 2))).isEqualByComparingTo("1169.50");
        assertThat(service.balanceAtEndOf(YearMonth.of(2025, 12))).isEqualByComparingTo("0");
    }

    @Test
    void apiSortsByAmount() {
        PageResult<TransactionApiData> page = service.apiTransactions(0, 2, "amount", SortDirection.ASC);

        assertThat(page.content()).extracting(TransactionApiData::amount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("-30.50"), new BigDecimal("-0.10"));
    }

    @Test
    void apiRejectsUnknownSortField() {
        assertThatThrownBy(() -> service.apiTransactions(0, 10, "password", SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pageBeyondTheEndIsEmpty() {
        assertThat(service.apiTransactions(5, 10, "date", SortDirection.ASC).content()).isEmpty();
    }

    private void add(String date, String amount, String message) {
        service.addTransaction(1, LocalDate.parse(date), new BigDecimal(amount), message);
    }
}
