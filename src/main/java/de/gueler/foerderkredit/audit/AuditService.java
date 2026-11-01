package de.gueler.foerderkredit.audit;

import de.gueler.foerderkredit.antrag.AntragService;
import de.gueler.foerderkredit.antrag.AntragStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Schreibt und liest das Audit-Log. */
@Service
public class AuditService {

    private final AuditRepository auditRepository;
    private final AntragService antragService;

    public AuditService(AuditRepository auditRepository, AntragService antragService) {
        this.auditRepository = auditRepository;
        this.antragService = antragService;
    }

    /**
     * Protokolliert einen Statuswechsel.
     *
     * <p>{@code Propagation.MANDATORY}: Diese Methode MUSS innerhalb einer bereits laufenden
     * Transaktion aufgerufen werden, sonst wirft Spring eine Exception. Damit ist garantiert,
     * dass Statuswechsel und Audit-Eintrag in derselben Transaktion landen: Entweder werden beide
     * gespeichert oder keins von beiden.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void protokolliere(UUID antragId, AntragStatus vonStatus, AntragStatus nachStatus,
                              String kommentar, String bearbeiter) {
        auditRepository.save(new AuditEintrag(antragId, vonStatus, nachStatus, kommentar, bearbeiter));
    }

    /** Alle Statuswechsel eines Antrags, ältester zuerst. 404, wenn es den Antrag nicht gibt. */
    @Transactional(readOnly = true)
    public List<AuditEintragResponse> historie(UUID antragId) {
        // Prüft, dass der Antrag existiert. Sonst wäre die Antwort eine leere Liste statt 404.
        antragService.findeAntrag(antragId);
        return auditRepository.findByAntragIdOrderByZeitpunktAsc(antragId).stream()
                .map(AuditEintragResponse::von)
                .toList();
    }
}
