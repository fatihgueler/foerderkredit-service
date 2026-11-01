package de.gueler.foerderkredit.tilgung;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Eine Monatsrate im Tilgungsplan.
 *
 * @param monat          laufende Nummer, beginnend bei 1
 * @param datum          Fälligkeitsdatum
 * @param zinsanteil     Zinsen dieses Monats
 * @param tilgungsanteil Teil der Rate, der die Schuld verringert
 * @param rate           Zinsanteil + Tilgungsanteil
 * @param restschuld     Schuld nach Zahlung dieser Rate
 */
public record Rate(
        int monat,
        LocalDate datum,
        BigDecimal zinsanteil,
        BigDecimal tilgungsanteil,
        BigDecimal rate,
        BigDecimal restschuld
) {
}
