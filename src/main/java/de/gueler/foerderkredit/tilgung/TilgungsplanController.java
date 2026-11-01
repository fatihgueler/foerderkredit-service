package de.gueler.foerderkredit.tilgung;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
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
    public Tilgungsplan tilgungsplan(@PathVariable UUID id) {
        return tilgungsplanService.berechne(id);
    }
}
