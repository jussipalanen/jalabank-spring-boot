package jussinet.jalabank.release.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.validation.Valid;
import jussinet.jalabank.release.model.Customer;
import jussinet.jalabank.release.model.DeleteResult;
import jussinet.jalabank.release.model.PageResult;
import jussinet.jalabank.release.model.StatementRow;
import jussinet.jalabank.release.model.Transaction;
import jussinet.jalabank.release.repository.CustomerRepository;
import jussinet.jalabank.release.service.TransactionService;
import jussinet.jalabank.release.web.TransactionForm;

/**
 * Main controller (for the web pages)
 */
@Controller
public class HomeController {

    static final List<Integer> PAGE_SIZES = List.of(5, 10, 25, 50, 100, 500, 1000);

    /** A month option in the balances filter */
    public record MonthOption(int number, String name) {
    }

    private final CustomerRepository customerRepository;
    private final TransactionService transactionService;

    HomeController(CustomerRepository customerRepository, TransactionService transactionService) {
        this.customerRepository = customerRepository;
        this.transactionService = transactionService;
    }

    /**
     * Dashboard
     */
    @GetMapping("/")
    public String index(Model model, @RequestParam(required = false) Long customerId) {
        Customer customer = selectCustomer(model, customerId);
        YearMonth thisMonth = YearMonth.from(transactionService.today());
        List<Transaction> scheduled = transactionService.scheduled(customer.id());

        model.addAttribute("balance", transactionService.currentBalance(customer.id()));
        model.addAttribute("summary", transactionService.monthSummary(customer.id(), thisMonth));
        model.addAttribute("thisMonth", monthName(thisMonth.getMonth()) + " " + thisMonth.getYear());
        model.addAttribute("recent", transactionService.recent(customer.id(), 8));
        model.addAttribute("scheduledCount", scheduled.size());
        model.addAttribute("scheduledTotal", scheduled.stream().map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return "index";
    }

    /**
     * Transaction form page
     */
    @GetMapping("/transaction")
    public String getTransaction(Model model, @RequestParam(required = false) Long customerId) {
        TransactionForm form = new TransactionForm();
        form.setDate(transactionService.today());
        form.setCustomerId(selectCustomer(model, customerId).id());
        model.addAttribute("transaction", form);
        return "transaction";
    }

    /**
     * Save a new transaction
     */
    @PostMapping("/transaction")
    public String postTransaction(Model model, @ModelAttribute("transaction") @Valid TransactionForm form,
            BindingResult result, RedirectAttributes redirect) {

        if (form.getCustomerId() != null && customerRepository.findById(form.getCustomerId()).isEmpty()) {
            result.rejectValue("customerId", "unknown", "Unknown customer");
        }
        if (!result.hasErrors()) {
            try {
                transactionService.addTransaction(form.getCustomerId(), form.getDate(), form.signedAmount(),
                        form.getMessage());
                redirect.addFlashAttribute("success", "Transaction added.");
                return "redirect:" + balancesUrl(form.getCustomerId(), YearMonth.from(form.getDate()), null, null);
            } catch (IllegalStateException e) {
                result.reject("full", e.getMessage());
            }
        }
        selectCustomer(model, form.getCustomerId());
        return "transaction";
    }

    /**
     * A customer's transactions of one month with cumulative balances, and the month's totals
     *
     * @param page one-based page number
     */
    @GetMapping("/balances")
    public String balances(Model model, @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {

        Customer customer = selectCustomer(model, customerId);
        LocalDate today = transactionService.today();
        int selectedMonth = month != null && month >= 1 && month <= 12 ? month : today.getMonthValue();
        int selectedYear = year != null && year >= 1 && year <= 9999 ? year : today.getYear();
        int pageSize = size != null && PAGE_SIZES.contains(size) ? size : 10;
        int pageIndex = page != null && page > 0 ? page - 1 : 0;

        YearMonth yearMonth = YearMonth.of(selectedYear, selectedMonth);
        PageResult<StatementRow> transactions = transactionService.statement(customer.id(), yearMonth, pageIndex,
                pageSize);

        List<Integer> years = new ArrayList<>(IntStream.rangeClosed(-1, 10).map(i -> today.getYear() - i)
                .boxed().toList());
        if (!years.contains(selectedYear)) {
            years.add(selectedYear);
        }

        model.addAttribute("transactions", transactions);
        model.addAttribute("summary", transactionService.monthSummary(customer.id(), yearMonth));
        model.addAttribute("monthName", monthName(yearMonth.getMonth()) + " " + selectedYear);
        model.addAttribute("previousMonth", yearMonth.minusMonths(1));
        model.addAttribute("nextMonth", yearMonth.plusMonths(1));
        model.addAttribute("years", years);
        model.addAttribute("months", Arrays.stream(Month.values())
                .map(m -> new MonthOption(m.getValue(), monthName(m)))
                .toList());
        model.addAttribute("pageSizes", PAGE_SIZES);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("size", pageSize);
        return "balances";
    }

    /**
     * Delete one transaction ({@code id}) or the selected ones ({@code ids}), then go back to the balances page
     */
    @PostMapping("/transactions/delete")
    public String deleteTransactions(@RequestParam(required = false) UUID id,
            @RequestParam(required = false) List<UUID> ids, @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            RedirectAttributes redirect) {

        List<UUID> toDelete = id != null ? List.of(id) : ids != null ? ids : List.of();
        if (toDelete.isEmpty()) {
            redirect.addFlashAttribute("warning", "Select at least one transaction to delete.");
        } else {
            DeleteResult result = transactionService.delete(toDelete);
            redirect.addFlashAttribute("success", result.deleted() == 1 ? "Deleted 1 transaction."
                    : "Deleted " + result.deleted() + " transactions.");
        }
        YearMonth yearMonth = month != null && year != null && month >= 1 && month <= 12 && year >= 1 && year <= 9999
                ? YearMonth.of(year, month)
                : null;
        return "redirect:" + balancesUrl(customerId, yearMonth, page, size);
    }

    /**
     * The old API docs page moved to the generated docs
     */
    @GetMapping("/apidocs")
    public String apiDocs() {
        return "redirect:/docs";
    }

    /**
     * Adds the customer list and the selected customer (the requested one, or the first) to the model
     */
    private Customer selectCustomer(Model model, Long customerId) {
        List<Customer> customers = customerRepository.findAll();
        Customer selected = customers.stream()
                .filter(c -> customerId != null && c.id() == customerId)
                .findFirst()
                .orElse(customers.getFirst());
        model.addAttribute("customers", customers);
        model.addAttribute("customer", selected);
        return selected;
    }

    private static String balancesUrl(Long customerId, YearMonth month, Integer page, Integer size) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath("/balances");
        if (customerId != null) {
            url.queryParam("customerId", customerId);
        }
        if (month != null) {
            url.queryParam("year", month.getYear()).queryParam("month", month.getMonthValue());
        }
        if (page != null) {
            url.queryParam("page", page);
        }
        if (size != null) {
            url.queryParam("size", size);
        }
        return url.toUriString();
    }

    private static String monthName(Month month) {
        return month.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
