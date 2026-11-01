package de.gueler.foerderkredit.regeln;

import static org.assertj.core.api.Assertions.assertThat;

import de.gueler.foerderkredit.antrag.AntragTestdaten;
import de.gueler.foerderkredit.antrag.Branche;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BranchenRegelTest {

    private final BranchenRegel regel = new BranchenRegel();

    @ParameterizedTest
    @EnumSource(value = Branche.class, names = {"GLUECKSSPIEL", "WAFFEN"}, mode = EnumSource.Mode.EXCLUDE)
    void istErfuelltFuerBranchenAusserhalbDerAusschlussliste(Branche branche) {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig().mitBranche(branche).baue());

        assertThat(ergebnis.erfuellt()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Branche.class, names = {"GLUECKSSPIEL", "WAFFEN"})
    void istNichtErfuelltFuerBranchenAufDerAusschlussliste(Branche branche) {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig().mitBranche(branche).baue());

        assertThat(ergebnis.erfuellt()).isFalse();
        assertThat(ergebnis.begruendung()).contains("ausgeschlossen");
    }
}
