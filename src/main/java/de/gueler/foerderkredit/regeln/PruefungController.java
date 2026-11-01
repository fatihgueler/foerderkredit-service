package de.gueler.foerderkredit.regeln;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-Endpunkt für die Förderprüfung. */
@RestController
@RequestMapping("/api/antraege")
@Tag(name = "Förderprüfung")
public class PruefungController {

    private final FoerderpruefungService foerderpruefungService;

    public PruefungController(FoerderpruefungService foerderpruefungService) {
        this.foerderpruefungService = foerderpruefungService;
    }

    @PostMapping("/{id}/pruefung")
    @Operation(summary = "Antrag gegen alle (fiktiven) Förderregeln prüfen")
    @ApiResponse(responseCode = "200", description = "Alle Regelergebnisse und das Gesamtergebnis")
    @ApiResponse(responseCode = "400", description = "Ungültige Eingabe (z. B. keine UUID)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Antrag nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public PruefungsErgebnis pruefe(@PathVariable UUID id) {
        return foerderpruefungService.pruefe(id);
    }
}
