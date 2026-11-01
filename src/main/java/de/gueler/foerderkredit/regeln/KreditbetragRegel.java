package de.gueler.foerderkredit.regeln;

import de.gueler.foerderkredit.antrag.Antrag;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** FIKTIVE Regel: Der Kreditbetrag muss zwischen 10.000 € und 2.000.000 € liegen (Grenzen inklusive). */
@Component
public class KreditbetragRegel implements Foerderregel {

    static final String NAME = "Kreditbetrag";
    static final BigDecimal MINIMUM = new BigDecimal("10000.00");
    static final BigDecimal MAXIMUM = new BigDecimal("2000000.00");

    @Override
    public RegelErgebnis pruefe(Antrag antrag) {
        BigDecimal betrag = antrag.getKreditbetrag();
        // compareTo statt equals: equals würde auch die Nachkommastellen vergleichen
        // (10000.0 ist laut equals ungleich 10000.00).
        boolean imRahmen = betrag.compareTo(MINIMUM) >= 0 && betrag.compareTo(MAXIMUM) <= 0;
        if (imRahmen) {
            return RegelErgebnis.erfuellt(NAME,
                    "Der Kreditbetrag von " + betrag + " € liegt zwischen 10.000 € und 2.000.000 €.");
        }
        return RegelErgebnis.nichtErfuellt(NAME,
                "Der Kreditbetrag von " + betrag + " € liegt nicht zwischen 10.000 € und 2.000.000 €.");
    }
}
