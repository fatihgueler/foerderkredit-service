package de.gueler.foerderkredit.workflow;

import de.gueler.foerderkredit.antrag.AntragResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
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
    public AntragResponse wechsleStatus(@PathVariable UUID id, @Valid @RequestBody StatuswechselRequest request) {
        return statuswechselService.wechsleStatus(id, request);
    }
}
