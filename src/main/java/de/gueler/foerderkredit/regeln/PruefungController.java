package de.gueler.foerderkredit.regeln;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
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
    public PruefungsErgebnis pruefe(@PathVariable UUID id) {
        return foerderpruefungService.pruefe(id);
    }
}
