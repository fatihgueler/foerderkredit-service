package de.gueler.foerderkredit.tilgung;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-Endpunkt für den Tilgungsplan. */
@RestController
@RequestMapping("/api/antraege")
@Tag(name = "Tilgungsplan")
public class TilgungsplanController {

    private final TilgungsplanService tilgungsplanService;

    public TilgungsplanController(TilgungsplanService tilgungsplanService) {
        this.tilgungsplanService = tilgungsplanService;
    }

    @GetMapping("/{id}/tilgungsplan")
    @Operation(summary = "Tilgungsplan (Annuitätendarlehen, monatlich) berechnen")
    @ApiResponse(responseCode = "200", description = "Tilgungsplan mit allen Monatsraten und Summen")
    @ApiResponse(responseCode = "400", description = "Ungültige Eingabe (z. B. keine UUID)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Antrag nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Mit den Antragsdaten ist kein Tilgungsplan berechenbar",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Tilgungsplan tilgungsplan(@PathVariable UUID id) {
        return tilgungsplanService.berechne(id);
    }
}
