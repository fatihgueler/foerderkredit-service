package de.gueler.foerderkredit.regeln;

import de.gueler.foerderkredit.antrag.Antrag;
import de.gueler.foerderkredit.antrag.Bundesland;
import org.springframework.stereotype.Component;

/** FIKTIVE Regel: Gefördert wird nur, wer seinen Sitz in Niedersachsen hat. */
@Component
public class StandortRegel implements Foerderregel {

    static final String NAME = "Standort";

    @Override
    public RegelErgebnis pruefe(Antrag antrag) {
        if (antrag.getBundesland() == Bundesland.NIEDERSACHSEN) {
            return RegelErgebnis.erfuellt(NAME, "Der Sitz liegt in Niedersachsen.");
        }
        return RegelErgebnis.nichtErfuellt(NAME, "Der Sitz liegt in " + antrag.getBundesland()
                + "; gefördert wird nur ein Sitz in Niedersachsen.");
    }
}
