package de.gueler.foerderkredit.workflow;

import de.gueler.foerderkredit.regeln.PruefungsErgebnis;
import de.gueler.foerderkredit.regeln.RegelErgebnis;
import java.util.List;

/**
 * Ein Antrag soll bewilligt werden, erfüllt aber nicht alle Förderregeln.
 * Ergibt HTTP 422 Unprocessable Entity.
 */
public class NichtFoerderfaehigException extends RuntimeException {

    private final transient PruefungsErgebnis pruefungsErgebnis;

    public NichtFoerderfaehigException(PruefungsErgebnis pruefungsErgebnis) {
        super("Der Antrag kann nicht bewilligt werden, weil er nicht alle Förderregeln erfüllt.");
        this.pruefungsErgebnis = pruefungsErgebnis;
    }

    public List<RegelErgebnis> getNichtErfuellteRegeln() {
        return pruefungsErgebnis.regelergebnisse().stream()
                .filter(ergebnis -> !ergebnis.erfuellt())
                .toList();
    }
}
