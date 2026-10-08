package jussinet.jalabank.release.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jussinet.jalabank.release.model.ApiPage;
import jussinet.jalabank.release.model.Customer;
import jussinet.jalabank.release.model.PageResult;
import jussinet.jalabank.release.repository.CustomerRepository;

/**
 * Customer API
 */
@RestController
@SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE)
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Read the demo customers")
public class CustomerApiController {

    private final CustomerRepository customerRepository;

    CustomerApiController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    @Operation(operationId = "listCustomers", summary = "List customers")
    ApiPage<Customer> list(
            @Parameter(description = "Page number, starting from 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size, 1–" + TransactionApiController.MAX_PAGE_SIZE)
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.clamp(size, 1, TransactionApiController.MAX_PAGE_SIZE);
        return ApiPage.from(PageResult.of(customerRepository.findAll(), Math.max(page, 1) - 1, pageSize));
    }

    @GetMapping("/me")
    @Operation(operationId = "getSignedInCustomer", summary = "Get the signed-in customer")
    Customer me(@AuthenticationPrincipal Customer customer) {
        return customer;
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getCustomer", summary = "Get a customer")
    @ApiResponse(responseCode = "200", description = "The customer")
    @ApiResponse(responseCode = "404", description = "No customer with this id")
    Customer get(@PathVariable long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
}
