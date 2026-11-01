package de.gueler.foerderkredit.regeln;

import static org.assertj.core.api.Assertions.assertThat;

import de.gueler.foerderkredit.antrag.AntragTestdaten;
import de.gueler.foerderkredit.antrag.Bundesland;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StandortRegelTest {

    private final StandortRegel regel = new StandortRegel();

    @Test
    void istErfuelltBeiSitzInNiedersachsen() {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig().mitBundesland(Bundesland.NIEDERSACHSEN).baue());

        assertThat(ergebnis.erfuellt()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Bundesland.class, names = "NIEDERSACHSEN", mode = EnumSource.Mode.EXCLUDE)
    void istNichtErfuelltBeiSitzInAllenAnderenBundeslaendern(Bundesland bundesland) {
        RegelErgebnis ergebnis = regel.pruefe(AntragTestdaten.gueltig().mitBundesland(bundesland).baue());

        assertThat(ergebnis.erfuellt()).isFalse();
        assertThat(ergebnis.begruendung()).contains(bundesland.name());
    }
}
