package de.gueler.foerderkredit.tilgung;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Reiner Unit-Test ohne Spring: Der Rechner wird einfach mit {@code new} erzeugt.
 */
class TilgungsplanRechnerTest {

    private static final LocalDate ERSTE_RATE = LocalDate.of(2027, 1, 1);

    private final TilgungsplanRechner rechner = new TilgungsplanRechner();

    /**
     * Von Hand nachgerechnetes Beispiel:
     * 100.000 € Kredit, 4 % Sollzins p. a., 10 Jahre Laufzeit, davon 1 Jahr tilgungsfrei.
     *
     * <pre>
     * 1) Monatszins
     *    i = 4 % / 12 = 0,04 / 12 = 0,0033333...
     *
     * 2) Tilgungsfreie Anlaufzeit: Monate 1 bis 12 (nur Zinsen)
     *    Zins = 100.000,00 * 0,0033333... = 333,3333... -> gerundet 333,33 €
     *    Tilgung = 0,00 €, Rate = 333,33 €, Restschuld bleibt 100.000,00 €
     *    Zinsen in der Anlaufzeit: 12 * 333,33 = 3.999,96 €
     *
     * 3) Annuität für die restlichen n = 9 * 12 = 108 Monate
     *    q = 1 + i = 1,0033333...
     *    q^108 = 1,4324715800...
     *    A = K * i * q^n / (q^n - 1)
     *      = 100.000 * 0,0033333... * 1,4324715800 / 0,4324715800
     *      = 333,3333... * 3,3122906...
     *      = 1.104,0968... -> gerundet 1.104,10 €
     *
     * 4) Monat 13 (erste Rate mit Tilgung)
     *    Zins    = 100.000,00 * 0,0033333... = 333,33 €
     *    Tilgung = 1.104,10 - 333,33         = 770,77 €
     *    Rest    = 100.000,00 - 770,77       = 99.229,23 €
     *
     * 5) Monat 14
     *    Zins    = 99.229,23 * 0,0033333... = 330,764... -> 330,76 €
     *    Tilgung = 1.104,10 - 330,76        = 773,34 €
     *    Rest    = 99.229,23 - 773,34       = 98.455,89 €
     *
     * 6) Monat 120 (letzte Rate): Restschuld vor der Rate 1.100,01 €
     *    Zins    = 1.100,01 * 0,0033333... = 3,6667 -> 3,67 €
     *    Tilgung = komplette Restschuld    = 1.100,01 €
     *    Rate    = 3,67 + 1.100,01         = 1.103,68 € (statt 1.104,10 -> Rundungsausgleich)
     *    Rest    = 0,00 €
     *
     * Die Monate 1-14 lassen sich mit dem Taschenrechner prüfen. Monat 120 und die
     * Gesamtzinsen (23.242,34 €) habe ich zusätzlich unabhängig mit Python (decimal,
     * 34 Stellen, ROUND_HALF_UP) nachgerechnet, nicht mit diesem Java-Code.
     * </pre>
     */
    @Nested
    class HandgerechnetesBeispiel {

        private final Tilgungsplan plan = rechner.berechne(
                new BigDecimal("100000.00"), new BigDecimal("4.000"), 10, 1, ERSTE_RATE);

        @Test
        void hat120MonatlicheRatenMitFortlaufendemDatum() {
            assertThat(plan.raten()).hasSize(120);
            assertThat(plan.raten().get(0).datum()).isEqualTo(LocalDate.of(2027, 1, 1));
            assertThat(plan.raten().get(1).datum()).isEqualTo(LocalDate.of(2027, 2, 1));
            assertThat(plan.raten().get(119).datum()).isEqualTo(LocalDate.of(2036, 12, 1));
        }

        @Test
        void berechnetInDenTilgungsfreienMonatenNurZinsen() {
            List<Rate> anlaufzeit = plan.raten().subList(0, 12);

            assertThat(anlaufzeit).allSatisfy(rate -> {
                assertThat(rate.zinsanteil()).isEqualByComparingTo("333.33");
                assertThat(rate.tilgungsanteil()).isEqualByComparingTo("0.00");
                assertThat(rate.rate()).isEqualByComparingTo("333.33");
                assertThat(rate.restschuld()).isEqualByComparingTo("100000.00");
            });
        }

        @Test
        void berechnetErsteAnnuitaetsrateWieVonHand() {
            Rate monat13 = plan.raten().get(12);

            assertThat(monat13.monat()).isEqualTo(13);
            assertThat(monat13.zinsanteil()).isEqualByComparingTo("333.33");
            assertThat(monat13.tilgungsanteil()).isEqualByComparingTo("770.77");
            assertThat(monat13.rate()).isEqualByComparingTo("1104.10");
            assertThat(monat13.restschuld()).isEqualByComparingTo("99229.23");
        }

        @Test
        void berechnetZweiteAnnuitaetsrateWieVonHand() {
            Rate monat14 = plan.raten().get(13);

            assertThat(monat14.zinsanteil()).isEqualByComparingTo("330.76");
            assertThat(monat14.tilgungsanteil()).isEqualByComparingTo("773.34");
            assertThat(monat14.restschuld()).isEqualByComparingTo("98455.89");
        }

        @Test
        void haeltDieAnnuitaetBisZurVorletztenRateKonstant() {
            assertThat(plan.raten().subList(12, 119))
                    .allSatisfy(rate -> assertThat(rate.rate()).isEqualByComparingTo("1104.10"));
        }

        @Test
        void letzteRateGleichtRundungsdifferenzAusUndRestschuldIstExaktNull() {
            Rate letzte = plan.raten().get(119);

            assertThat(letzte.zinsanteil()).isEqualByComparingTo("3.67");
            assertThat(letzte.tilgungsanteil()).isEqualByComparingTo("1100.01");
            assertThat(letzte.rate()).isEqualByComparingTo("1103.68");
            // Hier bewusst equals statt compareTo: genau "0.00" mit 2 Nachkommastellen.
            assertThat(letzte.restschuld()).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        void summeDerTilgungEntsprichtDemKreditbetrag() {
            assertThat(plan.gesamtTilgung()).isEqualByComparingTo("100000.00");
        }

        @Test
        void summeDerZinsenStimmtMitUnabhaengigerNachrechnungUeberein() {
            assertThat(plan.gesamtZinsen()).isEqualByComparingTo("23242.34");
        }

        @Test
        void summenStimmenMitDenEinzelratenUeberein() {
            BigDecimal summeZinsen = plan.raten().stream().map(Rate::zinsanteil).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal summeRaten = plan.raten().stream().map(Rate::rate).reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(plan.gesamtZinsen()).isEqualByComparingTo(summeZinsen);
            assertThat(summeRaten).isEqualByComparingTo(plan.gesamtZinsen().add(plan.gesamtTilgung()));
        }

        @Test
        void alleBetraegeHabenGenauZweiNachkommastellen() {
            assertThat(plan.raten()).allSatisfy(rate -> {
                assertThat(rate.zinsanteil().scale()).isEqualTo(2);
                assertThat(rate.tilgungsanteil().scale()).isEqualTo(2);
                assertThat(rate.rate().scale()).isEqualTo(2);
                assertThat(rate.restschuld().scale()).isEqualTo(2);
            });
        }
    }

    @Nested
    class Grenzfaelle {

        @Test
        void beginntOhneTilgungsfreieJahreSofortMitDerAnnuitaet() {
            // A = 100.000 * i * q^120 / (q^120 - 1) mit q^120 = 1,4908326824...
            //   = 1.012,4513... -> 1.012,45 € (unabhängig mit Python nachgerechnet)
            Tilgungsplan plan = rechner.berechne(new BigDecimal("100000.00"), new BigDecimal("4.000"), 10, 0, ERSTE_RATE);

            Rate erste = plan.raten().get(0);
            assertThat(plan.raten()).hasSize(120);
            assertThat(erste.zinsanteil()).isEqualByComparingTo("333.33");
            assertThat(erste.tilgungsanteil()).isEqualByComparingTo("679.12");
            assertThat(erste.rate()).isEqualByComparingTo("1012.45");
            assertThat(plan.raten().get(119).restschuld()).isEqualTo(new BigDecimal("0.00"));
            assertThat(plan.gesamtTilgung()).isEqualByComparingTo("100000.00");
        }

        @Test
        void rechnetBeiSehrKleinemZinsKorrektUndOhneDivisionsfehler() {
            // 0,001 % p. a.: Monatszins = 0,00000083333...; Zins im 1. Monat = 0,0833 -> 0,08 €
            Tilgungsplan plan = rechner.berechne(new BigDecimal("100000.00"), new BigDecimal("0.001"), 10, 1, ERSTE_RATE);

            assertThat(plan.raten().get(0).zinsanteil()).isEqualByComparingTo("0.08");
            assertThat(plan.gesamtZinsen()).isPositive().isLessThan(new BigDecimal("10.00"));
            assertThat(plan.gesamtTilgung()).isEqualByComparingTo("100000.00");
            assertThat(plan.raten().get(119).restschuld()).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        void teiltBeiNullProzentZinsDenBetragGleichmaessigAuf() {
            // 12.000 € auf 12 Monate ohne Zins: jede Rate genau 1.000,00 €
            Tilgungsplan plan = rechner.berechne(new BigDecimal("12000.00"), BigDecimal.ZERO, 1, 0, ERSTE_RATE);

            assertThat(plan.raten()).allSatisfy(rate -> {
                assertThat(rate.zinsanteil()).isEqualByComparingTo("0.00");
                assertThat(rate.rate()).isEqualByComparingTo("1000.00");
            });
            assertThat(plan.gesamtZinsen()).isEqualByComparingTo("0.00");
        }

        @Test
        void wirdBeiKleinstbetragNieNegativ() {
            // 2,00 € auf 240 Monate: Annuität 0,0083 -> aufgerundet 0,01 €.
            // Ohne Schutz würde nach 200 Monaten mehr getilgt als offen ist.
            Tilgungsplan plan = rechner.berechne(new BigDecimal("2.00"), BigDecimal.ZERO, 20, 0, ERSTE_RATE);

            assertThat(plan.raten()).allSatisfy(rate -> assertThat(rate.restschuld()).isNotNegative());
            assertThat(plan.gesamtTilgung()).isEqualByComparingTo("2.00");
        }

        @ParameterizedTest(name = "{0} € zu {1} % über {2} Jahre, {3} tilgungsfrei")
        @CsvSource({
                "10000.00,    1.500,  2, 0",
                "2000000.00,  7.250, 20, 3",
                "55555.55,    3.333,  7, 2",
                "123456.78,  12.000, 15, 1"
        })
        void endetImmerMitRestschuldNullUndVollstaendigerTilgung(String betrag, String zins, int laufzeit, int tilgungsfrei) {
            Tilgungsplan plan = rechner.berechne(new BigDecimal(betrag), new BigDecimal(zins), laufzeit, tilgungsfrei, ERSTE_RATE);

            assertThat(plan.raten()).hasSize(laufzeit * 12);
            assertThat(plan.raten().get(plan.raten().size() - 1).restschuld()).isEqualTo(new BigDecimal("0.00"));
            assertThat(plan.gesamtTilgung()).isEqualByComparingTo(betrag);
            assertThat(plan.raten()).allSatisfy(rate -> assertThat(rate.restschuld()).isNotNegative());
        }
    }

    @Nested
    class UngueltigeEingaben {

        @Test
        void lehntTilgungsfreieJahreGleichDerLaufzeitAb() {
            assertThatThrownBy(() -> rechner.berechne(new BigDecimal("10000.00"), new BigDecimal("2.0"), 3, 3, ERSTE_RATE))
                    .isInstanceOf(TilgungsplanNichtBerechenbarException.class)
                    .hasMessageContaining("tilgungsfreien Jahre");
        }

        @Test
        void lehntKreditbetragVonNullAb() {
            assertThatThrownBy(() -> rechner.berechne(BigDecimal.ZERO, new BigDecimal("2.0"), 3, 0, ERSTE_RATE))
                    .isInstanceOf(TilgungsplanNichtBerechenbarException.class);
        }

        @Test
        void lehntNegativenZinsAb() {
            assertThatThrownBy(() -> rechner.berechne(new BigDecimal("10000.00"), new BigDecimal("-0.5"), 3, 0, ERSTE_RATE))
                    .isInstanceOf(TilgungsplanNichtBerechenbarException.class);
        }
    }
}
