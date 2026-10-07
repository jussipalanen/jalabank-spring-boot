package jussinet.jalabank.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import jussinet.jalabank.release.service.TransactionService;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class ReleaseApplicationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private TransactionService transactionService;

	@Test
	void demoDataIsLoadedOnStartup() {
		assertThat(transactionService.count()).isGreaterThan(20);
	}

	@Test
	void pagesRender() {
		assertThat(mvc.get().uri("/")).hasStatusOk().bodyText().contains("your current balance is");
		assertThat(mvc.get().uri("/transaction")).hasStatusOk().bodyText().contains("john@doe.com");
		assertThat(mvc.get().uri("/apidocs")).hasStatusOk().bodyText().contains("/api/v1/transactions");
		assertThat(mvc.get().uri("/balances")).hasStatusOk().bodyText().contains("Salary", "Month balance");
		assertThat(mvc.get().uri("/balances?year=2000&month=13&size=7&page=99")).hasStatusOk()
				.bodyText().contains("No transactions in this month.");
	}

	@Test
	void addingATransactionUpdatesTheBalance() {
		var before = transactionService.currentBalance();

		assertThat(mvc.post().uri("/transaction")
				.param("amount", "12.34")
				.param("transferMethod", "WITHDRAW")
				.param("date", LocalDate.now().toString())
				.param("customerId", "1")
				.param("message", "Coffee"))
				.hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/");

		assertThat(transactionService.currentBalance()).isEqualByComparingTo(before.subtract(new java.math.BigDecimal("12.34")));
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
	void transactionsApi() {
		assertThat(mvc.get().uri("/api/v1/transactions?page=1&size=2&sortBy=amount&sortDirection=asc"))
				.hasStatusOk().bodyJson().extractingPath("$.length()").isEqualTo(2);
		assertThat(mvc.get().uri("/api/v1/transactions?size=1")).hasStatusOk().bodyText()
				.matches("\\[\\{\"id\":\"[0-9a-f-]+\",\"amount\":-?[0-9.]+,\"date\":\"\\d{4}-\\d{2}-\\d{2}\"}]");
		assertThat(mvc.get().uri("/api/v1/transactions?sortBy=password")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/v1/transactions?sortDirection=sideways")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/v1/transactions/count")).hasStatusOk();
	}

	@Test
	void customersApi() {
		assertThat(mvc.get().uri("/api/v1/customers/1")).hasStatusOk().bodyJson()
				.extractingPath("$.email").isEqualTo("john@doe.com");
		assertThat(mvc.get().uri("/api/v1/customers/999")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void bootstrapIsServedLocally() {
		assertThat(mvc.get().uri("/webjars/bootstrap/css/bootstrap.min.css")).hasStatusOk();
		assertThat(mvc.get().uri("/webjars/bootstrap/js/bootstrap.bundle.min.js")).hasStatusOk();
	}

	@Test
	void healthEndpointIsUp() {
		assertThat(mvc.get().uri("/actuator/health")).hasStatusOk().bodyJson()
				.extractingPath("$.status").isEqualTo("UP");
	}
}
