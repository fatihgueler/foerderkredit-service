package de.gueler.foerderkredit.regeln;

import de.gueler.foerderkredit.antrag.Antrag;
import de.gueler.foerderkredit.antrag.AntragService;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wendet ALLE Förderregeln auf einen Antrag an. Bricht nicht beim ersten Fehler ab,
 * damit der Sachbearbeiter auf einen Blick sieht, was alles nicht passt.
 */
@Service
public class FoerderpruefungService {

    private final AntragService antragService;
    private final List<Foerderregel> regeln;

    // Spring sucht alle Beans vom Typ Foerderregel und übergibt sie als Liste.
    public FoerderpruefungService(AntragService antragService, List<Foerderregel> regeln) {
        this.antragService = antragService;
        this.regeln = regeln;
    }

    @Transactional(readOnly = true)
    public PruefungsErgebnis pruefe(UUID antragId) {
        return pruefe(antragService.findeAntrag(antragId));
    }

    public PruefungsErgebnis pruefe(Antrag antrag) {
        List<RegelErgebnis> ergebnisse = regeln.stream()
                .map(regel -> regel.pruefe(antrag))
                // Feste Reihenfolge nach Name, damit die Antwort immer gleich aussieht.
                .sorted(Comparator.comparing(RegelErgebnis::regelName))
                .toList();
        boolean foerderfaehig = ergebnisse.stream().allMatch(RegelErgebnis::erfuellt);
        return new PruefungsErgebnis(antrag.getId(), foerderfaehig, ergebnisse);
    }
}
