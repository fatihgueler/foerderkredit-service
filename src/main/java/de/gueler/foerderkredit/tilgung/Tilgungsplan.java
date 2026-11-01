package de.gueler.foerderkredit.tilgung;

import java.math.BigDecimal;
import java.util.List;

/** Vollständiger Tilgungsplan: alle Monatsraten plus Summen. */
public record Tilgungsplan(List<Rate> raten, BigDecimal gesamtZinsen, BigDecimal gesamtTilgung) {
}
