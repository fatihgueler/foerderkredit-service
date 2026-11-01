package de.gueler.foerderkredit.regeln;

import static org.assertj.core.api.Assertions.assertThat;

import de.gueler.foerderkredit.antrag.AntragTestdaten;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LaufzeitRegelTest {

    private final LaufzeitRegel regel = new LaufzeitRegel();

    @ParameterizedTest(name = "Laufzeit {0} Jahre, {1} tilgungsfrei")
    @CsvSource({"2, 0", "2, 1", "10, 3", "20, 3", "20, 0"})
    void istErfuelltBeiZulaessigerLaufzeitUndTilgungsfreiheit(int laufzeit, int tilgungsfrei) {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig()
                .mitLaufzeitJahre(laufzeit).mitTilgungsfreienJahren(tilgungsfrei).baue());

        assertThat(ergebnis.erfuellt()).isTrue();
    }

    @Test
    void istNichtErfuelltBeiLaufzeitUnter2Jahren() {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig()
                .mitLaufzeitJahre(1).mitTilgungsfreienJahren(0).baue());

        assertThat(ergebnis.erfuellt()).isFalse();
        assertThat(ergebnis.begruendung()).contains("nicht zwischen 2 und 20 Jahren");
    }

    @Test
    void istNichtErfuelltBeiLaufzeitUeber20Jahren() {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig()
                .mitLaufzeitJahre(21).mitTilgungsfreienJahren(0).baue());

        assertThat(ergebnis.erfuellt()).isFalse();
    }

    @Test
    void istNichtErfuelltBeiMehrAls3TilgungsfreienJahren() {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig()
                .mitLaufzeitJahre(10).mitTilgungsfreienJahren(4).baue());

        assertThat(ergebnis.erfuellt()).isFalse();
        assertThat(ergebnis.begruendung()).contains("höchstens 3 tilgungsfreie Jahre");
    }

    @Test
    void istNichtErfuelltWennTilgungsfreieJahreGleichDerLaufzeitSind() {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig()
                .mitLaufzeitJahre(2).mitTilgungsfreienJahren(2).baue());

        assertThat(ergebnis.erfuellt()).isFalse();
        assertThat(ergebnis.begruendung()).contains("kleiner als die Laufzeit");
    }

    @Test
    void sammeltAlleVerstoesseInDerBegruendung() {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig()
                .mitLaufzeitJahre(1).mitTilgungsfreienJahren(5).baue());

        assertThat(ergebnis.begruendung())
                .contains("nicht zwischen 2 und 20 Jahren")
                .contains("höchstens 3 tilgungsfreie Jahre")
                .contains("kleiner als die Laufzeit");
    }
}
