package de.gueler.foerderkredit.regeln;

import static org.assertj.core.api.Assertions.assertThat;

import de.gueler.foerderkredit.antrag.AntragService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Prüft, dass Spring wirklich alle Regel-Klassen findet und als Liste injiziert.
 * Startet nur einen kleinen Spring-Kontext mit dem Paket {@code regeln}, ohne Datenbank.
 */
@SpringJUnitConfig(RegelRegistrierungTest.NurRegelnKonfiguration.class)
class RegelRegistrierungTest {

    @Configuration
    @ComponentScan(basePackageClasses = Foerderregel.class)
    static class NurRegelnKonfiguration {
    }

    @MockitoBean
    private AntragService antragService;

    @Autowired
    private List<Foerderregel> regeln;

    @Test
    void springInjiziertAlleFuenfFoerderregeln() {
        assertThat(regeln)
                .extracting(regel -> regel.getClass().getSimpleName())
                .containsExactlyInAnyOrder("KreditbetragRegel", "KmuRegel", "StandortRegel",
                        "BranchenRegel", "LaufzeitRegel");
    }
}
