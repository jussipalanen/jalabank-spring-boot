package jussinet.boobank.release.controller;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import jussinet.boobank.release.model.PageResult;
import jussinet.boobank.release.model.StatementRow;
import jussinet.boobank.release.repository.CustomerRepository;
import jussinet.boobank.release.service.TransactionService;
import jussinet.boobank.release.web.TransactionForm;

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
     * Home page
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("balance", transactionService.currentBalance());
        return "index";
    }

    /**
     * Transaction form page
     */
    @GetMapping("/transaction")
    public String getTransaction(Model model) {
        TransactionForm form = new TransactionForm();
        customerRepository.findAll().stream().findFirst().ifPresent(c -> form.setCustomerId(c.id()));
        model.addAttribute("transaction", form);
        model.addAttribute("customers", customerRepository.findAll());
        return "transaction";
    }

    /**
     * Save a new transaction
     */
    @PostMapping("/transaction")
    public String postTransaction(Model model, @ModelAttribute("transaction") @Valid TransactionForm form,
            BindingResult result) {

        if (form.getCustomerId() != null && customerRepository.findById(form.getCustomerId()).isEmpty()) {
            result.rejectValue("customerId", "unknown", "Unknown customer");
        }
        if (!result.hasErrors()) {
            try {
                transactionService.addTransaction(form.getCustomerId(), form.getDate(), form.signedAmount(),
                        form.getMessage());
                return "redirect:/";
            } catch (IllegalStateException e) {
                result.reject("full", e.getMessage());
            }
        }
        model.addAttribute("customers", customerRepository.findAll());
        return "transaction";
    }

    /**
     * API docs page
     */
    @GetMapping("/apidocs")
    public String getApiDocs(Model model) {
        String baseApiUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + "/api/v1";
        model.addAttribute("baseApiUrl", baseApiUrl);
        return "apidocs";
    }

    /**
     * The transactions of one month with cumulative balances, and the balance at the end of the month
     *
     * @param page one-based page number
     */
    @GetMapping("/balances")
    public String balances(Model model, @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        LocalDate today = LocalDate.now();
        int selectedMonth = month != null && month >= 1 && month <= 12 ? month : today.getMonthValue();
        int selectedYear = year != null && year >= 1 && year <= 9999 ? year : today.getYear();
        int pageSize = size != null && PAGE_SIZES.contains(size) ? size : 10;
        int pageIndex = page != null && page > 0 ? page - 1 : 0;

        YearMonth yearMonth = YearMonth.of(selectedYear, selectedMonth);
        PageResult<StatementRow> transactions = transactionService.statement(yearMonth, pageIndex, pageSize);

        model.addAttribute("transactions", transactions);
        model.addAttribute("monthBalance", transactionService.balanceAtEndOf(yearMonth));
        model.addAttribute("years", IntStream.rangeClosed(0, 10).map(i -> today.getYear() - i).boxed().toList());
        model.addAttribute("months", Arrays.stream(Month.values())
                .map(m -> new MonthOption(m.getValue(), m.getDisplayName(TextStyle.FULL, Locale.ENGLISH)))
                .toList());
        model.addAttribute("pageSizes", PAGE_SIZES);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("size", pageSize);
        return "balances";
    }
}
