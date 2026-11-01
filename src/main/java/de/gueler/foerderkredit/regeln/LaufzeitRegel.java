package de.gueler.foerderkredit.regeln;

import de.gueler.foerderkredit.antrag.Antrag;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * FIKTIVE Regel zu Laufzeit und tilgungsfreier Anlaufzeit:
 * <ul>
 *   <li>Laufzeit 2 bis 20 Jahre,</li>
 *   <li>höchstens 3 tilgungsfreie Jahre,</li>
 *   <li>tilgungsfreie Jahre kleiner als die Laufzeit (sonst würde nie getilgt).</li>
 * </ul>
 * Alle Verstöße werden in der Begründung gesammelt, nicht nur der erste.
 */
@Component
public class LaufzeitRegel implements Foerderregel {

    static final String NAME = "Laufzeit";
    static final int MIN_LAUFZEIT = 2;
    static final int MAX_LAUFZEIT = 20;
    static final int MAX_TILGUNGSFREIE_JAHRE = 3;

    @Override
    public RegelErgebnis pruefe(Antrag antrag) {
        int laufzeit = antrag.getLaufzeitJahre();
        int tilgungsfrei = antrag.getTilgungsfreieJahre();
        List<String> verstoesse = new ArrayList<>();

        if (laufzeit < MIN_LAUFZEIT || laufzeit > MAX_LAUFZEIT) {
            verstoesse.add("Die Laufzeit von " + laufzeit + " Jahren liegt nicht zwischen 2 und 20 Jahren.");
        }
        if (tilgungsfrei > MAX_TILGUNGSFREIE_JAHRE) {
            verstoesse.add("Es sind höchstens 3 tilgungsfreie Jahre erlaubt, beantragt sind " + tilgungsfrei + ".");
        }
        if (tilgungsfrei >= laufzeit) {
            verstoesse.add("Die tilgungsfreien Jahre müssen kleiner als die Laufzeit sein.");
        }

        if (verstoesse.isEmpty()) {
            return RegelErgebnis.erfuellt(NAME, "Laufzeit von " + laufzeit + " Jahren mit " + tilgungsfrei
                    + " tilgungsfreien Jahren ist zulässig.");
        }
        return RegelErgebnis.nichtErfuellt(NAME, String.join(" ", verstoesse));
    }
}
