package de.gueler.foerderkredit.antrag;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** REST-Endpunkte zum Anlegen und Lesen von Anträgen. */
@RestController
@RequestMapping("/api/antraege")
@Tag(name = "Anträge")
public class AntragController {

    private final AntragService antragService;

    public AntragController(AntragService antragService) {
        this.antragService = antragService;
    }

    @PostMapping
    @Operation(summary = "Neuen Antrag anlegen")
    @ApiResponse(responseCode = "201", description = "Antrag angelegt; Location-Header zeigt auf den neuen Antrag")
    @ApiResponse(responseCode = "400", description = "Validierungsfehler; Feld 'fehler' enthält eine Meldung pro Feld",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AntragResponse> anlegen(@Valid @RequestBody AntragAnlegenRequest request) {
        AntragResponse angelegt = antragService.anlegen(request);
        // 201 Created mit Location-Header, der auf den neuen Antrag zeigt.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(angelegt.id())
                .toUri();
        return ResponseEntity.created(location).body(angelegt);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Antrag lesen")
    @ApiResponse(responseCode = "200", description = "Antrag gefunden")
    @ApiResponse(responseCode = "400", description = "Ungültige Eingabe (z. B. keine UUID)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Antrag nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public AntragResponse lade(@PathVariable UUID id) {
        return antragService.lade(id);
    }

    @GetMapping
    @Operation(summary = "Anträge auflisten, optional nach Status gefiltert (paginiert)")
    @ApiResponse(responseCode = "200", description = "Eine Seite der Antragsliste")
    @ApiResponse(responseCode = "400", description = "Unbekannter Status oder unbekanntes Sortierfeld",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public AntragSeiteResponse suche(
            @RequestParam(required = false) AntragStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "erstelltAm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return antragService.suche(status, pageable);
    }
}
