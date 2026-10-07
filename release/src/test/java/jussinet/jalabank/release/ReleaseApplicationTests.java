package jussinet.jalabank.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import jussinet.jalabank.release.service.TransactionService;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class ReleaseApplicationTests {

	private static final long JOHN = 1;
	private static final long JANE = 2;

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private TransactionService transactionService;

	@Test
	void demoDataIsLoadedForBothCustomers() {
		assertThat(transactionService.recent(JOHN, 100)).hasSizeGreaterThan(20);
		assertThat(transactionService.recent(JANE, 100)).hasSizeGreaterThan(10);
	}

	@Test
	void pagesRender() {
		assertThat(mvc.get().uri("/")).hasStatusOk().bodyText().contains("Current balance", "Recent transactions");
		assertThat(mvc.get().uri("/?customerId=2")).hasStatusOk().bodyText().contains("Hello <span>Jane</span>");
		assertThat(mvc.get().uri("/transaction")).hasStatusOk().bodyText().contains("john@doe.com");
		assertThat(mvc.get().uri("/balances")).hasStatusOk().bodyText().contains("Salary", "Month-end balance",
				"Delete selected");
		assertThat(mvc.get().uri("/balances?year=2000&month=13&size=7&page=99")).hasStatusOk()
				.bodyText().contains("No transactions in this month.");
	}

	@Test
	void balancesPaginate() {
		// Last month is complete, so it has more than one page of five
		YearMonth lastMonth = YearMonth.now().minusMonths(1);
		assertThat(mvc.get().uri("/balances?customerId=1&size=5&page=2&year=" + lastMonth.getYear() + "&month="
				+ lastMonth.getMonthValue())).hasStatusOk().bodyText()
				.contains("Showing 6–10 of", "Next &raquo;", "&laquo; Previous", "aria-current=\"page\">2</a>");
	}

	@Test
	void addingATransactionUpdatesOnlyThatCustomersBalance() {
		BigDecimal john = transactionService.currentBalance(JOHN);
		BigDecimal jane = transactionService.currentBalance(JANE);

		assertThat(mvc.post().uri("/transaction")
				.param("amount", "12.34")
				.param("transferMethod", "WITHDRAW")
				.param("date", LocalDate.now().toString())
				.param("customerId", "2")
				.param("message", "Coffee"))
				.hasStatus(HttpStatus.FOUND).redirectedUrl().startsWith("/balances?customerId=2");

		assertThat(transactionService.currentBalance(JANE)).isEqualByComparingTo(jane.subtract(new BigDecimal("12.34")));
		assertThat(transactionService.currentBalance(JOHN)).isEqualByComparingTo(john);
	}

	@Test
	void futureTransactionIsScheduledAndDoesNotChangeTheCurrentBalance() {
		BigDecimal before = transactionService.currentBalance(JOHN);

		assertThat(mvc.post().uri("/transaction")
				.param("amount", "1000")
				.param("transferMethod", "WITHDRAW")
				.param("date", LocalDate.now().plusMonths(2).toString())
				.param("customerId", "1"))
				.hasStatus(HttpStatus.FOUND);

		assertThat(transactionService.currentBalance(JOHN)).isEqualByComparingTo(before);
		assertThat(transactionService.scheduled(JOHN)).isNotEmpty();
	}

	@Test
	void invalidTransactionShowsErrors() {
		assertThat(mvc.post().uri("/transaction")
				.param("amount", "-5")
				.param("transferMethod", "DEPOSIT")
				.param("date", LocalDate.now().toString())
				.param("customerId", "999"))
				.hasStatusOk().bodyText().contains("The amount must be at least 0.01", "Unknown customer");
	}

	@Test
	void deleteOneTransactionFromTheBalancesPage() {
		UUID id = addJaneTransaction("-1.00", "Delete me");

		assertThat(mvc.post().uri("/transactions/delete").param("id", id.toString()).param("customerId", "2"))
				.hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/balances?customerId=2");

		assertThat(transactionService.findById(id)).isEmpty();
	}

	@Test
	void deleteSeveralTransactionsFromTheBalancesPage() {
		UUID first = addJaneTransaction("-1.00", "Delete me 1");
		UUID second = addJaneTransaction("-2.00", "Delete me 2");
		UUID kept = addJaneTransaction("-3.00", "Keep me");
		YearMonth month = YearMonth.now();

		assertThat(mvc.post().uri("/transactions/delete")
				.param("ids", first.toString(), second.toString())
				.param("customerId", "2").param("year", String.valueOf(month.getYear()))
				.param("month", String.valueOf(month.getMonthValue())).param("page", "1").param("size", "10"))
				.hasStatus(HttpStatus.FOUND)
				.hasRedirectedUrl("/balances?customerId=2&year=" + month.getYear() + "&month=" + month.getMonthValue()
						+ "&page=1&size=10")
				.flash().containsEntry("success", "Deleted 2 transactions.");

		assertThat(transactionService.findById(first)).isEmpty();
		assertThat(transactionService.findById(second)).isEmpty();
		assertThat(transactionService.findById(kept)).isPresent();
	}

	@Test
	void deleteWithNothingSelectedWarns() {
		assertThat(mvc.post().uri("/transactions/delete")).hasStatus(HttpStatus.FOUND)
				.flash().containsEntry("warning", "Select at least one transaction to delete.");
	}

	@Test
	void transactionsApiIsPaged() {
		assertThat(mvc.get().uri("/api/v1/transactions?page=2&size=5&customerId=1&sortBy=amount&sortDirection=asc"))
				.hasStatusOk().bodyJson()
				.hasPathSatisfying("$.content.length()", v -> v.assertThat().isEqualTo(5))
				.hasPathSatisfying("$.page", v -> v.assertThat().isEqualTo(2))
				.hasPathSatisfying("$.size", v -> v.assertThat().isEqualTo(5))
				.hasPathSatisfying("$.content[0].customerId", v -> v.assertThat().isEqualTo(1))
				.hasPathSatisfying("$.totalPages", v -> v.assertThat().asNumber().isNotEqualTo(0));
		assertThat(mvc.get().uri("/api/v1/transactions?sortBy=password")).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.detail").asString().contains("sortBy");
		assertThat(mvc.get().uri("/api/v1/transactions?sortDirection=sideways")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/v1/transactions/count")).hasStatusOk();
	}

	@Test
	void transactionsApiCreateGetAndDelete() {
		var created = mvc.post().uri("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerId\": 2, \"amount\": -4.20, \"message\": \"Tea\"}").exchange();
		assertThat(created).hasStatus(HttpStatus.CREATED).bodyJson()
				.hasPathSatisfying("$.message", v -> v.assertThat().isEqualTo("Tea"))
				.hasPathSatisfying("$.date", v -> v.assertThat().isEqualTo(LocalDate.now().toString()));
		String location = created.getResponse().getHeader("Location");

		assertThat(mvc.get().uri(location)).hasStatusOk();
		assertThat(mvc.delete().uri(location)).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri(location)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.delete().uri(location)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void transactionsApiRejectsInvalidInput() {
		assertThat(mvc.post().uri("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerId\": 2, \"amount\": 0}")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.post().uri("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerId\": 999, \"amount\": 5}")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.post().uri("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\": 5}")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void transactionsApiDeletesSeveral() {
		UUID first = addJaneTransaction("-1.00", "API delete 1");
		UUID second = addJaneTransaction("-2.00", "API delete 2");
		UUID unknown = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/v1/transactions").param("ids", first.toString(), second.toString(),
				unknown.toString())).hasStatusOk().bodyJson()
				.hasPathSatisfying("$.deleted", v -> v.assertThat().isEqualTo(2))
				.hasPathSatisfying("$.notFound[0]", v -> v.assertThat().isEqualTo(unknown.toString()));
	}

	@Test
	void customersApiIsPaged() {
		assertThat(mvc.get().uri("/api/v1/customers?size=1&page=2")).hasStatusOk().bodyJson()
				.hasPathSatisfying("$.content[0].email", v -> v.assertThat().isEqualTo("jane@doe.com"))
				.hasPathSatisfying("$.totalElements", v -> v.assertThat().isEqualTo(2));
		assertThat(mvc.get().uri("/api/v1/customers/1")).hasStatusOk().bodyJson()
				.extractingPath("$.email").isEqualTo("john@doe.com");
		assertThat(mvc.get().uri("/api/v1/customers/999")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void apiHealth() {
		assertThat(mvc.get().uri("/api/health")).hasStatusOk().bodyJson()
				.hasPathSatisfying("$.status", v -> v.assertThat().isEqualTo("UP"))
				.hasPathSatisfying("$.application", v -> v.assertThat().isEqualTo("jalabank"));
		assertThat(mvc.get().uri("/actuator/health")).hasStatusOk().bodyJson()
				.extractingPath("$.status").isEqualTo("UP");
	}

	@Test
	void openApiDocs() {
		assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk().bodyText()
				.contains("Jalabank API", "/api/v1/transactions/{id}", "/api/v1/customers", "/api/health",
						"/actuator/health")
				.doesNotContain("/balances", "/transactions/delete");
		assertThat(mvc.get().uri("/docs")).hasStatus3xxRedirection();
		assertThat(mvc.get().uri("/apidocs")).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/docs");
	}

	@Test
	void staticAssetsAreServedLocally() {
		assertThat(mvc.get().uri("/webjars/bootstrap/css/bootstrap.min.css")).hasStatusOk();
		assertThat(mvc.get().uri("/webjars/bootstrap/js/bootstrap.bundle.min.js")).hasStatusOk();
		assertThat(mvc.get().uri("/css/app.css")).hasStatusOk();
		assertThat(mvc.get().uri("/js/app.js")).hasStatusOk();
	}

	private UUID addJaneTransaction(String amount, String message) {
		return transactionService.addTransaction(JANE, LocalDate.now(), new BigDecimal(amount), message).id();
	}

}
