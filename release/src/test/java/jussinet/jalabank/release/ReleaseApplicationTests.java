package jussinet.jalabank.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import jussinet.jalabank.release.repository.CustomerRepository;
import jussinet.jalabank.release.security.AuthenticatorService;
import jussinet.jalabank.release.security.CustomerAuthentication;
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

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private AuthenticatorService authenticatorService;

	@Test
	void demoDataIsLoadedForBothCustomers() {
		assertThat(transactionService.recent(JOHN, 100)).hasSizeGreaterThan(20);
		assertThat(transactionService.recent(JANE, 100)).hasSizeGreaterThan(10);
	}

	@Test
	void pagesRender() {
		assertThat(mvc.get().uri("/").with(signedIn(JOHN))).hasStatusOk().bodyText()
				.contains("Current balance", "Recent transactions", "Hello <span>John</span>", "Sign out");
		assertThat(mvc.get().uri("/").with(signedIn(JANE))).hasStatusOk().bodyText().contains("Hello <span>Jane</span>");
		assertThat(mvc.get().uri("/transaction").with(signedIn(JOHN))).hasStatusOk().bodyText().contains("john@doe.com");
		assertThat(mvc.get().uri("/balances").with(signedIn(JOHN))).hasStatusOk().bodyText()
				.contains("Salary", "Month-end balance", "Delete selected");
		assertThat(mvc.get().uri("/balances?year=2000&month=13&size=7&page=99").with(signedIn(JOHN))).hasStatusOk()
				.bodyText().contains("No transactions in this month.");
	}

	@Test
	void pagesShowOnlyTheSignedInCustomer() {
		assertThat(mvc.get().uri("/?customerId=2").with(signedIn(JOHN))).hasStatusOk().bodyText()
				.contains("Hello <span>John</span>").doesNotContain("Jane");
		assertThat(mvc.get().uri("/transaction").with(signedIn(JANE))).hasStatusOk().bodyText()
				.contains("jane@doe.com").doesNotContain("john@doe.com");
	}

	@Test
	void pagesNeedSignIn() {
		for (String page : new String[] { "/", "/balances", "/transaction" }) {
			assertThat(mvc.get().uri(page)).hasStatus(HttpStatus.FOUND).redirectedUrl().endsWith("/login");
		}
		assertThat(mvc.post().uri("/transactions/delete").with(csrf())).hasStatus(HttpStatus.FOUND)
				.redirectedUrl().endsWith("/login");
	}

	@Test
	void signInWithTheAuthenticatorCode() {
		MockHttpSession session = new MockHttpSession();
		assertThat(mvc.get().uri("/login").session(session)).hasStatusOk().bodyText()
				.contains("Sign in", "John Doe", "Jane Doe", "Log in");

		assertThat(mvc.post().uri("/login").session(session).with(csrf()).param("customerId", "2"))
				.hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/authenticator");

		// Choosing the customer alone does not sign in
		assertThat(mvc.get().uri("/").session(session)).hasStatus(HttpStatus.FOUND).redirectedUrl().endsWith("/login");
		assertThat(mvc.get().uri("/api/v1/customers/me").session(session)).hasStatus(HttpStatus.UNAUTHORIZED);

		assertThat(mvc.get().uri("/authenticator").session(session)).hasStatusOk().bodyText()
				.contains("Jane Doe", "Enter your code", "5 attempts left.", "/authenticator/codes", "Open authenticator");

		String code = authenticatorService.codes(JANE).getFirst().code();
		assertThat(mvc.get().uri("/authenticator/codes").session(session)).hasStatusOk().bodyText()
				.contains("Jalabank Authenticator", "Jane Doe", code, "Valid now", "Upcoming", "data-copy=\"" + code);

		assertThat(mvc.post().uri("/authenticator").session(session).with(csrf()).param("code", wrong(code)))
				.hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/authenticator")
				.flash().containsEntry("warning", "Wrong code. Check the code for the current hour and try again.");
		assertThat(mvc.get().uri("/authenticator").session(session)).hasStatusOk().bodyText().contains("4 attempts left.");

		assertThat(mvc.post().uri("/authenticator").session(session).with(csrf()).param("code", code))
				.hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/")
				.flash().containsEntry("success", "Welcome, Jane!");

		assertThat(mvc.get().uri("/").session(session)).hasStatusOk().bodyText()
				.contains("Hello <span>Jane</span>", "Signed in as");
		assertThat(mvc.get().uri("/api/v1/customers/me").session(session)).hasStatusOk().bodyJson()
				.extractingPath("$.email").isEqualTo("jane@doe.com");
		assertThat(mvc.get().uri("/login").session(session)).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/");

		// Signing out ends the session
		assertThat(mvc.post().uri("/logout").session(session).with(csrf())).hasStatus(HttpStatus.FOUND)
				.hasRedirectedUrl("/login?logout");
		assertThat(mvc.get().uri("/").session(new MockHttpSession())).hasStatus(HttpStatus.FOUND);
	}

	@Test
	void tooManyWrongCodesStartOver() {
		MockHttpSession session = new MockHttpSession();
		mvc.post().uri("/login").session(session).with(csrf()).param("customerId", "1").exchange();
		String wrong = wrong(authenticatorService.codes(JOHN).getFirst().code());

		for (int i = 1; i < 5; i++) {
			assertThat(mvc.post().uri("/authenticator").session(session).with(csrf()).param("code", wrong))
					.hasRedirectedUrl("/authenticator");
		}
		assertThat(mvc.post().uri("/authenticator").session(session).with(csrf()).param("code", wrong))
				.hasRedirectedUrl("/login")
				.flash().containsEntry("warning", "Too many wrong codes. Choose the customer and try again.");
		assertThat(mvc.get().uri("/authenticator").session(session)).hasRedirectedUrl("/login");
	}

	@Test
	void signInNeedsAKnownCustomerFirst() {
		assertThat(mvc.post().uri("/login").with(csrf()).param("customerId", "999")).hasRedirectedUrl("/login")
				.flash().containsEntry("warning", "Choose a customer to sign in.");
		assertThat(mvc.get().uri("/authenticator")).hasRedirectedUrl("/login");
		assertThat(mvc.post().uri("/authenticator").with(csrf()).param("code", "123456")).hasRedirectedUrl("/login");
		assertThat(mvc.get().uri("/authenticator/codes")).hasStatusOk().bodyText()
				.contains("No sign-in in progress").doesNotContainPattern("data-copy");
	}

	@Test
	void formsNeedACsrfToken() {
		assertThat(mvc.post().uri("/login").param("customerId", "1")).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.post().uri("/transaction").with(signedIn(JOHN)).param("amount", "1")
				.param("transferMethod", "DEPOSIT").param("date", LocalDate.now().toString()))
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void balancesPaginate() {
		// Last month is complete, so it has more than one page of five
		YearMonth lastMonth = YearMonth.now().minusMonths(1);
		assertThat(mvc.get().uri("/balances?size=5&page=2&year=" + lastMonth.getYear() + "&month="
				+ lastMonth.getMonthValue()).with(signedIn(JOHN))).hasStatusOk().bodyText()
				.contains("Showing 6–10 of", "Next &raquo;", "&laquo; Previous", "aria-current=\"page\">2</a>");
	}

	@Test
	void addingATransactionUpdatesOnlyThatCustomersBalance() {
		BigDecimal john = transactionService.currentBalance(JOHN);
		BigDecimal jane = transactionService.currentBalance(JANE);

		assertThat(mvc.post().uri("/transaction").with(signedIn(JANE)).with(csrf())
				.param("amount", "12.34")
				.param("transferMethod", "WITHDRAW")
				.param("date", LocalDate.now().toString())
				.param("customerId", "1")
				.param("message", "Coffee"))
				.hasStatus(HttpStatus.FOUND).redirectedUrl().startsWith("/balances?year=");

		assertThat(transactionService.currentBalance(JANE)).isEqualByComparingTo(jane.subtract(new BigDecimal("12.34")));
		assertThat(transactionService.currentBalance(JOHN)).isEqualByComparingTo(john);
	}

	@Test
	void futureTransactionIsScheduledAndDoesNotChangeTheCurrentBalance() {
		BigDecimal before = transactionService.currentBalance(JOHN);

		assertThat(mvc.post().uri("/transaction").with(signedIn(JOHN)).with(csrf())
				.param("amount", "1000")
				.param("transferMethod", "WITHDRAW")
				.param("date", LocalDate.now().plusMonths(2).toString()))
				.hasStatus(HttpStatus.FOUND);

		assertThat(transactionService.currentBalance(JOHN)).isEqualByComparingTo(before);
		assertThat(transactionService.scheduled(JOHN)).isNotEmpty();
	}

	@Test
	void invalidTransactionShowsErrors() {
		assertThat(mvc.post().uri("/transaction").with(signedIn(JOHN)).with(csrf())
				.param("amount", "-5")
				.param("transferMethod", "DEPOSIT")
				.param("date", LocalDate.now().toString()))
				.hasStatusOk().bodyText().contains("The amount must be at least 0.01", "john@doe.com");
	}

	@Test
	void deleteOneTransactionFromTheBalancesPage() {
		UUID id = addJaneTransaction("-1.00", "Delete me");

		assertThat(mvc.post().uri("/transactions/delete").with(signedIn(JANE)).with(csrf()).param("id", id.toString()))
				.hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/balances");

		assertThat(transactionService.findById(id)).isEmpty();
	}

	@Test
	void cannotDeleteAnotherCustomersTransaction() {
		UUID id = addJaneTransaction("-1.00", "Not John's");

		assertThat(mvc.post().uri("/transactions/delete").with(signedIn(JOHN)).with(csrf()).param("id", id.toString()))
				.hasStatus(HttpStatus.FOUND).flash().containsEntry("success", "Deleted 0 transactions.");

		assertThat(transactionService.findById(id)).isPresent();
	}

	@Test
	void deleteSeveralTransactionsFromTheBalancesPage() {
		UUID first = addJaneTransaction("-1.00", "Delete me 1");
		UUID second = addJaneTransaction("-2.00", "Delete me 2");
		UUID kept = addJaneTransaction("-3.00", "Keep me");
		YearMonth month = YearMonth.now();

		assertThat(mvc.post().uri("/transactions/delete").with(signedIn(JANE)).with(csrf())
				.param("ids", first.toString(), second.toString())
				.param("year", String.valueOf(month.getYear()))
				.param("month", String.valueOf(month.getMonthValue())).param("page", "1").param("size", "10"))
				.hasStatus(HttpStatus.FOUND)
				.hasRedirectedUrl("/balances?year=" + month.getYear() + "&month=" + month.getMonthValue()
						+ "&page=1&size=10")
				.flash().containsEntry("success", "Deleted 2 transactions.");

		assertThat(transactionService.findById(first)).isEmpty();
		assertThat(transactionService.findById(second)).isEmpty();
		assertThat(transactionService.findById(kept)).isPresent();
	}

	@Test
	void deleteWithNothingSelectedWarns() {
		assertThat(mvc.post().uri("/transactions/delete").with(signedIn(JOHN)).with(csrf())).hasStatus(HttpStatus.FOUND)
				.flash().containsEntry("warning", "Select at least one transaction to delete.");
	}

	@Test
	void transactionsApiIsPaged() {
		assertThat(mvc.get().uri("/api/v1/transactions?page=2&size=5&customerId=1&sortBy=amount&sortDirection=asc")
				.with(signedIn(JOHN))).hasStatusOk().bodyJson()
				.hasPathSatisfying("$.content.length()", v -> v.assertThat().isEqualTo(5))
				.hasPathSatisfying("$.page", v -> v.assertThat().isEqualTo(2))
				.hasPathSatisfying("$.size", v -> v.assertThat().isEqualTo(5))
				.hasPathSatisfying("$.content[0].customerId", v -> v.assertThat().isEqualTo(1))
				.hasPathSatisfying("$.totalPages", v -> v.assertThat().asNumber().isNotEqualTo(0));
		assertThat(mvc.get().uri("/api/v1/transactions?sortBy=password").with(signedIn(JOHN)))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.detail").asString().contains("sortBy");
		assertThat(mvc.get().uri("/api/v1/transactions?sortDirection=sideways").with(signedIn(JOHN)))
				.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/v1/transactions/count").with(signedIn(JANE))).hasStatusOk().bodyJson()
				.extractingPath("$").isEqualTo(transactionService.count(JANE));
	}

	@Test
	void apiNeedsSignIn() {
		assertThat(mvc.get().uri("/api/v1/transactions")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.get().uri("/api/v1/customers")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.delete().uri("/api/v1/transactions").param("ids", UUID.randomUUID().toString()))
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void apiShowsOnlyTheSignedInCustomersTransactions() {
		UUID janes = addJaneTransaction("-1.00", "Jane's coffee");

		assertThat(mvc.get().uri("/api/v1/transactions?size=1000").with(signedIn(JOHN))).hasStatusOk().bodyJson()
				.extractingPath("$.content[*].customerId").asArray().containsOnly(1);
		assertThat(mvc.get().uri("/api/v1/transactions?customerId=2").with(signedIn(JOHN)))
				.hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.get().uri("/api/v1/transactions/" + janes).with(signedIn(JOHN))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.delete().uri("/api/v1/transactions/" + janes).with(signedIn(JOHN)))
				.hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.post().uri("/api/v1/transactions").with(signedIn(JOHN)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerId\": 2, \"amount\": 5}")).hasStatus(HttpStatus.FORBIDDEN);

		assertThat(transactionService.findById(janes)).isPresent();
		assertThat(mvc.get().uri("/api/v1/transactions/" + janes).with(signedIn(JANE))).hasStatusOk();
	}

	@Test
	void transactionsApiCreateGetAndDelete() {
		var created = mvc.post().uri("/api/v1/transactions").with(signedIn(JANE)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\": -4.20, \"message\": \"Tea\"}").exchange();
		assertThat(created).hasStatus(HttpStatus.CREATED).bodyJson()
				.hasPathSatisfying("$.message", v -> v.assertThat().isEqualTo("Tea"))
				.hasPathSatisfying("$.customerId", v -> v.assertThat().isEqualTo(2))
				.hasPathSatisfying("$.date", v -> v.assertThat().isEqualTo(LocalDate.now().toString()));
		String location = created.getResponse().getHeader("Location");

		assertThat(mvc.get().uri(location).with(signedIn(JANE))).hasStatusOk();
		assertThat(mvc.delete().uri(location).with(signedIn(JANE))).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri(location).with(signedIn(JANE))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.delete().uri(location).with(signedIn(JANE))).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void transactionsApiRejectsInvalidInput() {
		assertThat(mvc.post().uri("/api/v1/transactions").with(signedIn(JANE)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\": 0}")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.post().uri("/api/v1/transactions").with(signedIn(JANE)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerId\": 2}")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void transactionsApiDeletesSeveral() {
		UUID first = addJaneTransaction("-1.00", "API delete 1");
		UUID second = addJaneTransaction("-2.00", "API delete 2");
		UUID unknown = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/v1/transactions").with(signedIn(JANE)).param("ids", first.toString(),
				second.toString(), unknown.toString())).hasStatusOk().bodyJson()
				.hasPathSatisfying("$.deleted", v -> v.assertThat().isEqualTo(2))
				.hasPathSatisfying("$.notFound[0]", v -> v.assertThat().isEqualTo(unknown.toString()));
	}

	@Test
	void customersApiIsPaged() {
		assertThat(mvc.get().uri("/api/v1/customers?size=1&page=2").with(signedIn(JOHN))).hasStatusOk().bodyJson()
				.hasPathSatisfying("$.content[0].email", v -> v.assertThat().isEqualTo("jane@doe.com"))
				.hasPathSatisfying("$.totalElements", v -> v.assertThat().isEqualTo(2));
		assertThat(mvc.get().uri("/api/v1/customers/1").with(signedIn(JOHN))).hasStatusOk().bodyJson()
				.extractingPath("$.email").isEqualTo("john@doe.com");
		assertThat(mvc.get().uri("/api/v1/customers/me").with(signedIn(JANE))).hasStatusOk().bodyJson()
				.extractingPath("$.email").isEqualTo("jane@doe.com");
		assertThat(mvc.get().uri("/api/v1/customers/999").with(signedIn(JOHN))).hasStatus(HttpStatus.NOT_FOUND);
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
						"/actuator/health", "JSESSIONID")
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

	/** Signs the request in as the customer, as if they had entered the right authenticator code */
	private RequestPostProcessor signedIn(long customerId) {
		return authentication(CustomerAuthentication.of(customerRepository.findById(customerId).orElseThrow()));
	}

	/** A 6-digit code that is not {@code code} */
	private static String wrong(String code) {
		return code.equals("000000") ? "111111" : "000000";
	}

	private UUID addJaneTransaction(String amount, String message) {
		return transactionService.addTransaction(JANE, LocalDate.now(), new BigDecimal(amount), message).id();
	}

}
