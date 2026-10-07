package jussinet.jalabank.release.model;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The outcome of deleting transactions.
 */
@Schema(description = "The outcome of deleting transactions")
public record DeleteResult(
        @Schema(description = "Number of deleted transactions", example = "2") int deleted,
        @Schema(description = "Requested ids that did not exist") List<UUID> notFound) {
}
