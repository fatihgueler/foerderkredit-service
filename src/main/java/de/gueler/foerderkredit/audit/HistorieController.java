package de.gueler.foerderkredit.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
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
    public List<AuditEintragResponse> historie(@PathVariable UUID id) {
        return auditService.historie(id);
    }
}
