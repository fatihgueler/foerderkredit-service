package de.gueler.foerderkredit.tilgung;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Berechnet den Tilgungsplan eines Annuitätendarlehens mit monatlichen Raten.
 *
 * <p>Reine Java-Klasse ohne Spring: Sie hat keinen Zustand, liest nichts aus der
 * Datenbank und ist deshalb mit einfachen Unit-Tests vollständig prüfbar.
 *
 * <p>Ablauf:
 * <ol>
 *   <li>Tilgungsfreie Anlaufzeit: Es werden nur Zinsen gezahlt, die Schuld bleibt gleich.</li>
 *   <li>Danach eine konstante Monatsrate (Annuität) über die Restlaufzeit. Mit jeder Rate
 *       sinkt der Zinsanteil und der Tilgungsanteil steigt.</li>
 *   <li>Die letzte Rate tilgt genau die verbliebene Restschuld. So werden Rundungsdifferenzen
 *       ausgeglichen und die Restschuld ist am Ende exakt 0,00.</li>
 * </ol>
 *
 * <p>Geld wird ausschließlich mit {@link BigDecimal} gerechnet, niemals mit double/float.
 * Zwischenergebnisse (Monatszins, Potenz, Annuität) laufen mit {@link MathContext#DECIMAL128}
 * (34 signifikante Stellen). Jeder Geldbetrag, der im Plan steht, wird auf 2 Nachkommastellen
 * mit {@link RoundingMode#HALF_UP} (kaufmännisch) gerundet.
 */
public class TilgungsplanRechner {

    private static final MathContext GENAUIGKEIT = MathContext.DECIMAL128;
    private static final BigDecimal HUNDERT = new BigDecimal("100");
    private static final BigDecimal ZWOELF = new BigDecimal("12");

    /**
     * @param kreditbetrag       ausgezahlter Betrag, größer als 0
     * @param sollzinsProzent    Jahreszins in Prozent, z. B. 4.0 für 4 %; 0 ist erlaubt
     * @param laufzeitJahre      Gesamtlaufzeit inklusive tilgungsfreier Jahre
     * @param tilgungsfreieJahre Jahre, in denen nur Zinsen gezahlt werden; kleiner als die Laufzeit
     * @param ersteRateAm        Fälligkeit der ersten Rate; jede weitere Rate einen Monat später
     */
    public Tilgungsplan berechne(BigDecimal kreditbetrag,
                                 BigDecimal sollzinsProzent,
                                 int laufzeitJahre,
                                 int tilgungsfreieJahre,
                                 LocalDate ersteRateAm) {
        pruefeEingaben(kreditbetrag, sollzinsProzent, laufzeitJahre, tilgungsfreieJahre);

        int monateGesamt = laufzeitJahre * 12;
        int monateTilgungsfrei = tilgungsfreieJahre * 12;
        int monateMitTilgung = monateGesamt - monateTilgungsfrei;

        // Monatszins als Dezimalzahl: 4 % pro Jahr -> 0,04 / 12 = 0,003333...
        BigDecimal monatszins = sollzinsProzent.divide(HUNDERT, GENAUIGKEIT).divide(ZWOELF, GENAUIGKEIT);
        BigDecimal annuitaet = berechneAnnuitaet(kreditbetrag, monatszins, monateMitTilgung);

        List<Rate> raten = new ArrayList<>(monateGesamt);
        BigDecimal restschuld = kreditbetrag.setScale(2, RoundingMode.HALF_UP);
        BigDecimal summeZinsen = BigDecimal.ZERO;
        BigDecimal summeTilgung = BigDecimal.ZERO;

        for (int monat = 1; monat <= monateGesamt; monat++) {
            BigDecimal zinsanteil = runde(restschuld.multiply(monatszins, GENAUIGKEIT));
            BigDecimal tilgungsanteil;

            if (monat <= monateTilgungsfrei) {
                // Anlaufzeit: nur Zinsen, keine Tilgung.
                tilgungsanteil = BigDecimal.ZERO.setScale(2);
            } else if (monat == monateGesamt) {
                // Letzte Rate: genau die Restschuld tilgen (gleicht Rundungsdifferenzen aus).
                tilgungsanteil = restschuld;
            } else {
                // Normale Rate: Annuität minus Zinsen. min(...) verhindert bei Kleinstbeträgen,
                // dass durch Aufrunden der Annuität mehr getilgt wird, als noch offen ist.
                tilgungsanteil = annuitaet.subtract(zinsanteil).min(restschuld);
            }

            restschuld = restschuld.subtract(tilgungsanteil);
            summeZinsen = summeZinsen.add(zinsanteil);
            summeTilgung = summeTilgung.add(tilgungsanteil);

            raten.add(new Rate(
                    monat,
                    ersteRateAm.plusMonths(monat - 1L),
                    zinsanteil,
                    tilgungsanteil,
                    zinsanteil.add(tilgungsanteil),
                    restschuld));
        }

        return new Tilgungsplan(List.copyOf(raten), summeZinsen, summeTilgung);
    }

    /**
     * Annuitätenformel: A = K * i * q^n / (q^n - 1) mit q = 1 + i.
     * K = Kreditbetrag, i = Monatszins, n = Anzahl Monate mit Tilgung.
     * Bei 0 % Zins wäre das „0 / 0“, dann ist die Rate einfach K / n.
     */
    private static BigDecimal berechneAnnuitaet(BigDecimal kreditbetrag, BigDecimal monatszins, int monate) {
        BigDecimal anzahlMonate = BigDecimal.valueOf(monate);
        if (monatszins.signum() == 0) {
            return runde(kreditbetrag.divide(anzahlMonate, GENAUIGKEIT));
        }
        BigDecimal q = BigDecimal.ONE.add(monatszins);
        BigDecimal qHochN = q.pow(monate, GENAUIGKEIT);
        BigDecimal zaehler = kreditbetrag.multiply(monatszins, GENAUIGKEIT).multiply(qHochN, GENAUIGKEIT);
        BigDecimal nenner = qHochN.subtract(BigDecimal.ONE, GENAUIGKEIT);
        return runde(zaehler.divide(nenner, GENAUIGKEIT));
    }

    private static BigDecimal runde(BigDecimal betrag) {
        return betrag.setScale(2, RoundingMode.HALF_UP);
    }

    private static void pruefeEingaben(BigDecimal kreditbetrag, BigDecimal sollzinsProzent,
                                       int laufzeitJahre, int tilgungsfreieJahre) {
        if (kreditbetrag == null || kreditbetrag.signum() <= 0) {
            throw new TilgungsplanNichtBerechenbarException("Der Kreditbetrag muss größer als 0 sein.");
        }
        if (sollzinsProzent == null || sollzinsProzent.signum() < 0) {
            throw new TilgungsplanNichtBerechenbarException("Der Sollzins darf nicht negativ sein.");
        }
        if (laufzeitJahre < 1) {
            throw new TilgungsplanNichtBerechenbarException("Die Laufzeit muss mindestens 1 Jahr betragen.");
        }
        if (tilgungsfreieJahre < 0 || tilgungsfreieJahre >= laufzeitJahre) {
            throw new TilgungsplanNichtBerechenbarException(
                    "Die tilgungsfreien Jahre müssen zwischen 0 und der Laufzeit minus 1 liegen.");
        }
    }
}
