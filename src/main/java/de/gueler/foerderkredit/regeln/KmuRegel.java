package de.gueler.foerderkredit.regeln;

import de.gueler.foerderkredit.antrag.Antrag;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * FIKTIVE Regel: Nur kleine und mittlere Unternehmen (KMU) werden gefördert,
 * also unter 250 Mitarbeitende UND Jahresumsatz bis 50 Mio. €.
 */
@Component
public class KmuRegel implements Foerderregel {

    static final String NAME = "KMU";
    static final int MITARBEITER_GRENZE = 250;
    static final BigDecimal UMSATZ_GRENZE = new BigDecimal("50000000.00");

    @Override
    public RegelErgebnis pruefe(Antrag antrag) {
        boolean wenigerAls250 = antrag.getMitarbeiterAnzahl() < MITARBEITER_GRENZE;
        boolean umsatzBis50Mio = antrag.getJahresumsatz().compareTo(UMSATZ_GRENZE) <= 0;

        if (wenigerAls250 && umsatzBis50Mio) {
            return RegelErgebnis.erfuellt(NAME, "Das Unternehmen ist ein KMU (unter 250 Mitarbeitende, "
                    + "Jahresumsatz bis 50 Mio. €).");
        }
        if (!wenigerAls250) {
            return RegelErgebnis.nichtErfuellt(NAME, "Das Unternehmen hat " + antrag.getMitarbeiterAnzahl()
                    + " Mitarbeitende; erlaubt sind weniger als 250.");
        }
        return RegelErgebnis.nichtErfuellt(NAME, "Der Jahresumsatz von " + antrag.getJahresumsatz()
                + " € liegt über 50 Mio. €.");
    }
}
