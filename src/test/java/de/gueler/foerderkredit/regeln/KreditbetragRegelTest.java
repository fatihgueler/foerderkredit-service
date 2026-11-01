package de.gueler.foerderkredit.regeln;

import static org.assertj.core.api.Assertions.assertThat;

import de.gueler.foerderkredit.antrag.AntragTestdaten;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KreditbetragRegelTest {

    private final KreditbetragRegel regel = new KreditbetragRegel();

    @ParameterizedTest
    @ValueSource(strings = {"10000.00", "100000.00", "2000000.00"})
    void istErfuelltWennKreditbetragImRahmenLiegtInklusiveGrenzen(String betrag) {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig().mitKreditbetrag(betrag).baue());

        assertThat(ergebnis.erfuellt()).isTrue();
        assertThat(ergebnis.regelName()).isEqualTo("Kreditbetrag");
    }

    @ParameterizedTest
    @ValueSource(strings = {"9999.99", "2000000.01", "5000.00"})
    void istNichtErfuelltWennKreditbetragAusserhalbDesRahmensLiegt(String betrag) {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig().mitKreditbetrag(betrag).baue());

        assertThat(ergebnis.erfuellt()).isFalse();
        assertThat(ergebnis.begruendung()).contains(betrag).contains("nicht zwischen");
    }

    @Test
    void nenntDenBetragAuchInDerPositivenBegruendung() {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig().mitKreditbetrag("50000.00").baue());

        assertThat(ergebnis.begruendung()).contains("50000.00");
    }
}
