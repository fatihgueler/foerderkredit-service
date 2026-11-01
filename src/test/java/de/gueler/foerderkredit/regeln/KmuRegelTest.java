package de.gueler.foerderkredit.regeln;

import static org.assertj.core.api.Assertions.assertThat;

import de.gueler.foerderkredit.antrag.AntragTestdaten;
import org.junit.jupiter.api.Test;

class KmuRegelTest {

    private final KmuRegel regel = new KmuRegel();

    @Test
    void istErfuelltBei249MitarbeitendenUndGenau50MioUmsatz() {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig()
                .mitMitarbeiterAnzahl(249)
                .mitJahresumsatz("50000000.00")
                .baue());

        assertThat(ergebnis.erfuellt()).isTrue();
    }

    @Test
    void istNichtErfuelltBeiGenau250Mitarbeitenden() {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig().mitMitarbeiterAnzahl(250).baue());

        assertThat(ergebnis.erfuellt()).isFalse();
        assertThat(ergebnis.begruendung()).contains("250 Mitarbeitende");
    }

    @Test
    void istNichtErfuelltWennJahresumsatzUeber50MioLiegt() {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig().mitJahresumsatz("50000000.01").baue());

        assertThat(ergebnis.erfuellt()).isFalse();
        assertThat(ergebnis.begruendung()).contains("über 50 Mio.");
    }
}
