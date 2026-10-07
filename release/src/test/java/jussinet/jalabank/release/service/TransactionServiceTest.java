package jussinet.jalabank.release.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jussinet.jalabank.release.model.DeleteResult;
import jussinet.jalabank.release.model.MonthSummary;
import jussinet.jalabank.release.model.PageResult;
import jussinet.jalabank.release.model.SortDirection;
import jussinet.jalabank.release.model.StatementRow;
import jussinet.jalabank.release.model.Transaction;
import jussinet.jalabank.release.model.TransactionApiData;
import jussinet.jalabank.release.repository.TransactionRepository;

class TransactionServiceTest {

    private static final long JOHN = 1;
    private static final long JANE = 2;

    /** "Today" in these tests */
    private static final LocalDate TODAY = LocalDate.parse("2026-03-10");

    private TransactionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new TransactionService(new TransactionRepository(), clock);
        // Added out of date order on purpose
        add(JOHN, "2026-02-10", "-30.50", "Groceries");
        add(JOHN, "2026-01-01", "1000.00", "Opening");
        add(JANE, "2026-02-05", "500.00", "Jane's deposit");
        add(JOHN, "2026-02-01", "200.00", "Salary");
        add(JOHN, "2026-03-05", "-0.10", "Fee");
        add(JOHN, "2026-04-01", "-300.00", "Future rent");
    }

    @Test
    void currentBalanceCountsOnlyTheCustomersTransactionsUpToToday() {
        assertThat(service.currentBalance(JOHN)).isEqualByComparingTo("1169.40");
        assertThat(service.currentBalance(JANE)).isEqualByComparingTo("500.00");
    }

    @Test
    void futureTransactionsAreScheduled() {
        assertThat(service.scheduled(JOHN)).extracting(Transaction::message).containsExactly("Future rent");
        assertThat(service.balanceOn(JOHN, LocalDate.parse("2026-04-30"))).isEqualByComparingTo("869.40");
    }

    @Test
    void statementShowsOnlyTheCustomersMonthWithRunningBalance() {
        PageResult<StatementRow> page = service.statement(JOHN, YearMonth.of(2026, 2), 0, 10);

        assertThat(page.content()).extracting(StatementRow::message).containsExactly("Salary", "Groceries");
        assertThat(page.content()).extracting(StatementRow::cumulativeBalance)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1200.00"), new BigDecimal("1169.50"));
    }

    @Test
    void sameDayTransactionsKeepTheOrderTheyWereAddedIn() {
        add(JANE, "2026-02-05", "-100.00", "Same day, added later");

        assertThat(service.statement(JANE, YearMonth.of(2026, 2), 0, 10).content())
                .extracting(StatementRow::cumulativeBalance)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("500.00"), new BigDecimal("400.00"));
    }

    @Test
    void everyCumulativeBalanceEqualsTheSumOfThatCustomersTransactionsSoFar() {
        // Independent check: recompute each row by summing all earlier transactions of the same customer
        List<Transaction> all = List.of(
                new Transaction(UUID.randomUUID(), JOHN, LocalDate.parse("2026-01-01"), new BigDecimal("1000.00"), ""),
                new Transaction(UUID.randomUUID(), JOHN, LocalDate.parse("2026-02-01"), new BigDecimal("200.00"), ""),
                new Transaction(UUID.randomUUID(), JOHN, LocalDate.parse("2026-02-10"), new BigDecimal("-30.50"), ""),
                new Transaction(UUID.randomUUID(), JOHN, LocalDate.parse("2026-03-05"), new BigDecimal("-0.10"), ""),
                new Transaction(UUID.randomUUID(), JOHN, LocalDate.parse("2026-04-01"), new BigDecimal("-300.00"), ""));
        for (int i = 0; i < all.size(); i++) {
            Transaction t = all.get(i);
            BigDecimal expected = all.subList(0, i + 1).stream().map(Transaction::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            StatementRow row = service.statement(JOHN, YearMonth.from(t.date()), 0, 100).content().stream()
                    .filter(r -> r.amount().compareTo(t.amount()) == 0).findFirst().orElseThrow();
            assertThat(row.cumulativeBalance()).as("after %s", t.date()).isEqualByComparingTo(expected);
        }
    }

    @Test
    void monthSummary() {
        MonthSummary february = service.monthSummary(JOHN, YearMonth.of(2026, 2));

        assertThat(february.income()).isEqualByComparingTo("200.00");
        assertThat(february.expenses()).isEqualByComparingTo("-30.50");
        assertThat(february.endBalance()).isEqualByComparingTo("1169.50");
        assertThat(february.count()).isEqualTo(2);
        assertThat(service.monthSummary(JOHN, YearMonth.of(2025, 12)).endBalance()).isEqualByComparingTo("0");
    }

    @Test
    void recentIsNewestFirstAndSkipsFutureTransactions() {
        assertThat(service.recent(JOHN, 2)).extracting(StatementRow::message).containsExactly("Fee", "Groceries");
    }

    @Test
    void statementPaginatesAndClampsToTheLastPage() {
        PageResult<StatementRow> page = service.statement(JOHN, YearMonth.of(2026, 2), 1, 1);
        assertThat(page.content()).extracting(StatementRow::message).containsExactly("Groceries");
        assertThat(page.totalPages()).isEqualTo(2);

        assertThat(service.statement(JOHN, YearMonth.of(2026, 2), 9, 1).currentPage()).isEqualTo(2);
    }

    @Test
    void deleteRemovesOnlyTheGivenTransactionsAndReportsUnknownIds() {
        List<UUID> february = service.statement(JOHN, YearMonth.of(2026, 2), 0, 10).content().stream()
                .map(StatementRow::id).toList();
        UUID unknown = UUID.randomUUID();

        DeleteResult result = service.delete(List.of(february.get(0), february.get(1), unknown));

        assertThat(result.deleted()).isEqualTo(2);
        assertThat(result.notFound()).containsExactly(unknown);
        assertThat(service.statement(JOHN, YearMonth.of(2026, 2), 0, 10).totalElements()).isZero();
        assertThat(service.currentBalance(JOHN)).isEqualByComparingTo("999.90");
        assertThat(service.currentBalance(JANE)).isEqualByComparingTo("500.00");
    }

    @Test
    void apiFiltersByCustomerAndSorts() {
        PageResult<TransactionApiData> page = service.apiTransactions(JOHN, 0, 2, "amount", SortDirection.ASC);

        assertThat(page.content()).extracting(TransactionApiData::amount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("-300.00"), new BigDecimal("-30.50"));
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(service.apiTransactions(null, 0, 100, "date", SortDirection.ASC).totalElements()).isEqualTo(6);
    }

    @Test
    void apiRejectsUnknownSortField() {
        assertThatThrownBy(() -> service.apiTransactions(null, 0, 10, "password", SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pageWindowShowsEllipsisForGaps() {
        PageResult<Integer> page = PageResult.of(java.util.stream.IntStream.range(0, 120).boxed().toList(), 5, 10);

        assertThat(page.pageWindow()).containsExactly(1, null, 4, 5, 6, 7, 8, null, 12);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    private void add(long customerId, String date, String amount, String message) {
        service.addTransaction(customerId, LocalDate.parse(date), new BigDecimal(amount), message);
    }
}
