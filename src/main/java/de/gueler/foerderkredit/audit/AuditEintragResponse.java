package de.gueler.foerderkredit.audit;

import de.gueler.foerderkredit.antrag.AntragStatus;
import java.time.Instant;
import java.util.UUID;

/** Antwort-DTO für einen Eintrag der Historie. */
public record AuditEintragResponse(
        UUID id,
        UUID antragId,
        AntragStatus vonStatus,
        AntragStatus nachStatus,
        String kommentar,
        String bearbeiter,
        Instant zeitpunkt
) {

    public static AuditEintragResponse von(AuditEintrag eintrag) {
        return new AuditEintragResponse(
                eintrag.getId(),
                eintrag.getAntragId(),
                eintrag.getVonStatus(),
                eintrag.getNachStatus(),
                eintrag.getKommentar(),
                eintrag.getBearbeiter(),
                eintrag.getZeitpunkt());
    }
}
