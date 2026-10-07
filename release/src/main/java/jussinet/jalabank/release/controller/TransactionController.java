package jussinet.jalabank.release.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jussinet.jalabank.release.model.SortDirection;
import jussinet.jalabank.release.model.TransactionApiData;
import jussinet.jalabank.release.service.TransactionService;

/**
 * Transaction API
 */
@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private final TransactionService transactionService;

    TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Get the transactions. All request parameters are optional; without {@code size} every transaction is returned.
     *
     * @param page one-based page number
     */
    @GetMapping("/transactions")
    List<TransactionApiData> all(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        int pageIndex = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : Math.max(transactionService.count(), 1);
        try {
            SortDirection direction = SortDirection.parse(sortDirection);
            return transactionService.apiTransactions(pageIndex, pageSize, sortBy, direction).content();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Get the total count of the transactions
     */
    @GetMapping("/transactions/count")
    int count() {
        return transactionService.count();
    }
}
