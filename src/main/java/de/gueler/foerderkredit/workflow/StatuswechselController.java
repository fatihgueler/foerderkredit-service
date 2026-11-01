package de.gueler.foerderkredit.workflow;

import de.gueler.foerderkredit.antrag.AntragResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-Endpunkt für Statuswechsel im Workflow. */
@RestController
@RequestMapping("/api/antraege")
@Tag(name = "Workflow")
public class StatuswechselController {

    private final StatuswechselService statuswechselService;

    public StatuswechselController(StatuswechselService statuswechselService) {
        this.statuswechselService = statuswechselService;
    }

    @PostMapping("/{id}/statuswechsel")
    @Operation(summary = "Status eines Antrags ändern (409 bei verbotenem Übergang, 422 wenn nicht förderfähig)")
    @ApiResponse(responseCode = "200", description = "Status geändert; Antwort ist der aktualisierte Antrag")
    @ApiResponse(responseCode = "400", description = "Validierungsfehler, z. B. Bearbeiter fehlt",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Antrag nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Übergang im Zustandsautomaten nicht erlaubt, oder jemand anderes hat den Antrag gleichzeitig geändert",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Bewilligung nicht möglich, weil der Antrag nicht förderfähig ist",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public AntragResponse wechsleStatus(@PathVariable UUID id, @Valid @RequestBody StatuswechselRequest request) {
        return statuswechselService.wechsleStatus(id, request);
    }
}
