package de.gueler.foerderkredit.tilgung;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gueler.foerderkredit.antrag.Antrag;
import de.gueler.foerderkredit.antrag.AntragService;
import de.gueler.foerderkredit.antrag.AntragTestdaten;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TilgungsplanServiceTest {

    @Test
    void ersteRateIstAmErstenDesMonatsNachAntragstellung() {
        AntragService antragService = mock(AntragService.class);
        Antrag antrag = AntragTestdaten.gueltig().baue();
        UUID id = UUID.randomUUID();
        when(antragService.findeAntrag(id)).thenReturn(antrag);

        Tilgungsplan plan = new TilgungsplanService(antragService).berechne(id);

        LocalDate erstellt = LocalDate.ofInstant(antrag.getErstelltAm(), ZoneId.of("Europe/Berlin"));
        assertThat(plan.raten().get(0).datum()).isEqualTo(erstellt.withDayOfMonth(1).plusMonths(1));
        assertThat(plan.raten()).hasSize(antrag.getLaufzeitJahre() * 12);
    }
}
