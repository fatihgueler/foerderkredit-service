package de.gueler.foerderkredit.workflow;

import de.gueler.foerderkredit.antrag.Antrag;
import de.gueler.foerderkredit.antrag.AntragResponse;
import de.gueler.foerderkredit.antrag.AntragService;
import de.gueler.foerderkredit.antrag.AntragStatus;
import de.gueler.foerderkredit.audit.AuditService;
import de.gueler.foerderkredit.regeln.FoerderpruefungService;
import de.gueler.foerderkredit.regeln.PruefungsErgebnis;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Führt einen Statuswechsel durch und protokolliert ihn im Audit-Log. */
@Service
public class StatuswechselService {

    private final AntragService antragService;
    private final FoerderpruefungService foerderpruefungService;
    private final AuditService auditService;

    public StatuswechselService(AntragService antragService,
                                FoerderpruefungService foerderpruefungService,
                                AuditService auditService) {
        this.antragService = antragService;
        this.foerderpruefungService = foerderpruefungService;
        this.auditService = auditService;
    }

    /**
     * Ablauf in EINER Transaktion:
     * <ol>
     *   <li>Antrag laden (404, wenn unbekannt).</li>
     *   <li>Übergang gegen den Zustandsautomaten prüfen (409, wenn verboten).</li>
     *   <li>Bei BEWILLIGT: Förderregeln prüfen (422, wenn nicht förderfähig).</li>
     *   <li>Status setzen und Audit-Eintrag schreiben.</li>
     * </ol>
     * Wirft einer der Schritte eine Exception, rollt Spring die Transaktion zurück:
     * Es gibt dann weder einen Statuswechsel noch einen Audit-Eintrag.
     */
    @Transactional
    public AntragResponse wechsleStatus(UUID antragId, StatuswechselRequest request) {
        Antrag antrag = antragService.findeAntrag(antragId);
        AntragStatus vonStatus = antrag.getStatus();
        AntragStatus nachStatus = request.neuerStatus();

        if (!vonStatus.darfWechselnZu(nachStatus)) {
            throw new UngueltigerStatuswechselException(vonStatus, nachStatus);
        }

        if (nachStatus == AntragStatus.BEWILLIGT) {
            PruefungsErgebnis pruefung = foerderpruefungService.pruefe(antrag);
            if (!pruefung.foerderfaehig()) {
                throw new NichtFoerderfaehigException(pruefung);
            }
        }

        // Kein save() nötig: Der Antrag ist in dieser Transaktion von JPA "verwaltet".
        // Hibernate erkennt die Änderung und schreibt sie beim Commit (Dirty Checking).
        antrag.wechsleStatus(nachStatus);
        auditService.protokolliere(antragId, vonStatus, nachStatus, request.kommentar(), request.bearbeiter());

        return AntragResponse.von(antrag);
    }
}
