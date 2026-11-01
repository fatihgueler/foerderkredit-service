package de.gueler.foerderkredit.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-Endpunkt für die Historie (Audit-Log) eines Antrags. */
@RestController
@RequestMapping("/api/antraege")
@Tag(name = "Historie")
public class HistorieController {

    private final AuditService auditService;

    public HistorieController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/{id}/historie")
    @Operation(summary = "Alle Statuswechsel eines Antrags (ältester zuerst)")
    @ApiResponse(responseCode = "200", description = "Historie des Antrags")
    @ApiResponse(responseCode = "400", description = "Ungültige Eingabe (z. B. keine UUID)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Antrag nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public List<AuditEintragResponse> historie(@PathVariable UUID id) {
        return auditService.historie(id);
    }
}
