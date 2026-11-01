package de.gueler.foerderkredit.regeln;

import de.gueler.foerderkredit.antrag.Antrag;

/**
 * Eine einzelne Förderregel (Strategy-Pattern).
 *
 * <p>Jede Regel ist eine eigene Klasse mit {@code @Component}. Spring findet alle
 * Implementierungen automatisch und übergibt sie als {@code List<Foerderregel>} an den
 * {@link FoerderpruefungService}. Eine neue Regel = eine neue Klasse; bestehender Code
 * muss dafür nicht geändert werden (Open-Closed-Prinzip).
 *
 * <p><strong>Alle Regeln in diesem Projekt sind FIKTIV</strong> und entsprechen keinem
 * echten Förderprogramm.
 */
public interface Foerderregel {

    RegelErgebnis pruefe(Antrag antrag);
}
