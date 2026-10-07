package jussinet.jalabank.release.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jussinet.jalabank.release.model.ApiPage;
import jussinet.jalabank.release.model.DeleteResult;
import jussinet.jalabank.release.model.SortDirection;
import jussinet.jalabank.release.model.Transaction;
import jussinet.jalabank.release.model.TransactionApiData;
import jussinet.jalabank.release.repository.CustomerRepository;
import jussinet.jalabank.release.service.TransactionService;

/**
 * Transaction API
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "List, add and delete transactions")
public class TransactionApiController {

    static final int MAX_PAGE_SIZE = 1000;

    private final TransactionService transactionService;
    private final CustomerRepository customerRepository;

    TransactionApiController(TransactionService transactionService, CustomerRepository customerRepository) {
        this.transactionService = transactionService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    @Operation(operationId = "listTransactions",
            summary = "List transactions", description = "One page of transactions, sorted as requested.")
    @ApiResponse(responseCode = "200", description = "One page of transactions")
    @ApiResponse(responseCode = "400", description = "Unknown sortBy or sortDirection")
    ApiPage<TransactionApiData> list(
            @Parameter(description = "Only this customer's transactions")
            @RequestParam(required = false) Long customerId,
            @Parameter(description = "Page number, starting from 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size, 1–" + MAX_PAGE_SIZE) @RequestParam(defaultValue = "20") int size,
            @Parameter(schema = @Schema(allowableValues = { "date", "amount", "id" }))
            @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(schema = @Schema(allowableValues = { "DESC", "ASC" }))
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        int pageSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        try {
            SortDirection direction = SortDirection.parse(sortDirection);
            return ApiPage.from(transactionService.apiTransactions(customerId, Math.max(page, 1) - 1, pageSize,
                    sortBy, direction));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getTransaction", summary = "Get a transaction")
    @ApiResponse(responseCode = "200", description = "The transaction")
    @ApiResponse(responseCode = "404", description = "No transaction with this id")
    TransactionApiData get(@PathVariable UUID id) {
        return transactionService.findById(id).map(TransactionApiData::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    @GetMapping("/count")
    @Operation(operationId = "countTransactions", summary = "Count all transactions")
    int count() {
        return transactionService.count();
    }

    @PostMapping
    @Operation(operationId = "createTransaction",
            summary = "Add a transaction", description = "Use a positive amount for a deposit and a negative "
            + "amount for a withdrawal.")
    @ApiResponse(responseCode = "201", description = "The added transaction")
    @ApiResponse(responseCode = "400", description = "Invalid transaction, or unknown customer")
    ResponseEntity<TransactionApiData> create(@Valid @RequestBody TransactionRequest request) {
        if (customerRepository.findById(request.customerId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown customer " + request.customerId());
        }
        LocalDate date = request.date() != null ? request.date() : transactionService.today();
        Transaction saved;
        try {
            saved = transactionService.addTransaction(request.customerId(), date, request.amount(), request.message());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + saved.id()))
                .body(TransactionApiData.from(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteTransaction", summary = "Delete a transaction")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "No transaction with this id")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (transactionService.delete(List.of(id)).deleted() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found");
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(operationId = "deleteTransactions",
            summary = "Delete several transactions", description = "Deletes every listed transaction that exists "
            + "and reports the ids that did not.")
    @ApiResponse(responseCode = "200", description = "How many were deleted, and which ids were not found")
    DeleteResult deleteMany(@Parameter(description = "Ids of the transactions to delete", required = true)
            @RequestParam List<UUID> ids) {
        return transactionService.delete(ids);
    }
}
