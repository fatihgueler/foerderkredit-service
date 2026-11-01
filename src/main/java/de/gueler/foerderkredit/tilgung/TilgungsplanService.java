package de.gueler.foerderkredit.tilgung;

import de.gueler.foerderkredit.antrag.Antrag;
import de.gueler.foerderkredit.antrag.AntragService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verbindet den Antrag aus der Datenbank mit dem reinen {@link TilgungsplanRechner}. */
@Service
public class TilgungsplanService {

    private static final ZoneId ZEITZONE = ZoneId.of("Europe/Berlin");

    private final AntragService antragService;
    // Der Rechner hat keinen Zustand und keine Abhängigkeiten, daher einfach mit new erzeugt.
    private final TilgungsplanRechner rechner = new TilgungsplanRechner();

    public TilgungsplanService(AntragService antragService) {
        this.antragService = antragService;
    }

    /**
     * Annahme: Auszahlung im Monat der Antragstellung, erste Rate am 1. des Folgemonats.
     * So ist der Plan für denselben Antrag immer gleich, egal wann er abgerufen wird.
     */
    @Transactional(readOnly = true)
    public Tilgungsplan berechne(UUID antragId) {
        Antrag antrag = antragService.findeAntrag(antragId);
        LocalDate ersteRateAm = LocalDate.ofInstant(antrag.getErstelltAm(), ZEITZONE)
                .withDayOfMonth(1)
                .plusMonths(1);
        return rechner.berechne(
                antrag.getKreditbetrag(),
                antrag.getSollzinsProzent(),
                antrag.getLaufzeitJahre(),
                antrag.getTilgungsfreieJahre(),
                ersteRateAm);
    }
}
