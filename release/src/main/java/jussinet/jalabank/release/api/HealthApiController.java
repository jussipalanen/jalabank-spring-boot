package jussinet.jalabank.release.api;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Health check API. Uses the same health status as the actuator's /actuator/health.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Is the application up")
public class HealthApiController {

    @Schema(name = "Health", description = "Application health")
    public record HealthResponse(
            @Schema(example = "UP") String status,
            @Schema(example = "jalabank") String application,
            @Schema(example = "2.3.0") String version,
            @Schema(description = "Time since the application started, in seconds", example = "3600")
            long uptimeSeconds,
            @Schema(example = "2026-10-07T21:30:00Z") Instant time) {
    }

    private final HealthEndpoint healthEndpoint;
    private final ObjectProvider<BuildProperties> buildProperties;

    HealthApiController(HealthEndpoint healthEndpoint, ObjectProvider<BuildProperties> buildProperties) {
        this.healthEndpoint = healthEndpoint;
        this.buildProperties = buildProperties;
    }

    @GetMapping
    @Operation(operationId = "apiHealth", summary = "Check the application health")
    @ApiResponse(responseCode = "200", description = "The application is up")
    @ApiResponse(responseCode = "503", description = "The application is not healthy")
    ResponseEntity<HealthResponse> health() {
        Status status = healthEndpoint.health().getStatus();
        BuildProperties build = buildProperties.getIfAvailable();
        HealthResponse body = new HealthResponse(
                status.getCode(),
                build != null ? build.getArtifact() : "jalabank",
                build != null ? build.getVersion() : "unknown",
                Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()).toSeconds(),
                Instant.now());
        HttpStatus httpStatus = Status.UP.equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(body);
    }
}
