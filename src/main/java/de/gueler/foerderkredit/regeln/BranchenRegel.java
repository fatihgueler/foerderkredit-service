package de.gueler.foerderkredit.regeln;

import de.gueler.foerderkredit.antrag.Antrag;
import de.gueler.foerderkredit.antrag.Branche;
import java.util.Set;
import org.springframework.stereotype.Component;

/** FIKTIVE Regel: Bestimmte Branchen sind von der Förderung ausgeschlossen. */
@Component
public class BranchenRegel implements Foerderregel {

    static final String NAME = "Branche";
    static final Set<Branche> AUSSCHLUSSLISTE = Set.of(Branche.GLUECKSSPIEL, Branche.WAFFEN);

    @Override
    public RegelErgebnis pruefe(Antrag antrag) {
        if (AUSSCHLUSSLISTE.contains(antrag.getBranche())) {
            return RegelErgebnis.nichtErfuellt(NAME, "Die Branche " + antrag.getBranche()
                    + " ist von der Förderung ausgeschlossen.");
        }
        return RegelErgebnis.erfuellt(NAME, "Die Branche " + antrag.getBranche()
                + " ist nicht von der Förderung ausgeschlossen.");
    }
}
