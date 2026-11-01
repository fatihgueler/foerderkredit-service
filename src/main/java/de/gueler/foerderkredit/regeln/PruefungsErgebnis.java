package de.gueler.foerderkredit.regeln;

import java.util.List;
import java.util.UUID;

/**
 * Gesamtergebnis der Förderprüfung: alle Einzelergebnisse und ob der Antrag
 * insgesamt förderfähig ist (nur wenn ALLE Regeln erfüllt sind).
 */
public record PruefungsErgebnis(UUID antragId, boolean foerderfaehig, List<RegelErgebnis> regelergebnisse) {
}
