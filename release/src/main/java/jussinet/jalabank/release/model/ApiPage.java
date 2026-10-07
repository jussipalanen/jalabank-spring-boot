package jussinet.jalabank.release.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One page of a list, as returned by the REST API.
 */
@JsonPropertyOrder({ "content", "page", "size", "totalElements", "totalPages" })
@Schema(description = "One page of results")
public record ApiPage<T>(
        @Schema(description = "The items on this page") List<T> content,
        @Schema(description = "Page number, starting from 1", example = "1") int page,
        @Schema(description = "Maximum number of items per page", example = "20") int size,
        @Schema(description = "Number of items on all pages", example = "66") long totalElements,
        @Schema(description = "Number of pages", example = "4") int totalPages) {

    public static <T> ApiPage<T> from(PageResult<T> page) {
        return new ApiPage<>(page.content(), page.currentPage(), page.size(), page.totalElements(), page.totalPages());
    }
}
