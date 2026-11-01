package de.gueler.foerderkredit.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gueler.foerderkredit.antrag.Antrag;
import de.gueler.foerderkredit.antrag.AntragNichtGefundenException;
import de.gueler.foerderkredit.antrag.AntragResponse;
import de.gueler.foerderkredit.antrag.AntragService;
import de.gueler.foerderkredit.antrag.AntragStatus;
import de.gueler.foerderkredit.antrag.AntragTestdaten;
import de.gueler.foerderkredit.audit.AuditService;
import de.gueler.foerderkredit.regeln.FoerderpruefungService;
import de.gueler.foerderkredit.regeln.PruefungsErgebnis;
import de.gueler.foerderkredit.regeln.RegelErgebnis;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatuswechselServiceTest {

    @Mock
    private AntragService antragService;
    @Mock
    private FoerderpruefungService foerderpruefungService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private StatuswechselService statuswechselService;

    private final UUID antragId = UUID.randomUUID();
    private Antrag antrag;

    @BeforeEach
    void antragImStatusEingereicht() {
        antrag = AntragTestdaten.gueltig().baue();
    }

    @Test
    void wechseltErlaubtenStatusUndSchreibtAuditEintrag() {
        when(antragService.findeAntrag(antragId)).thenReturn(antrag);

        AntragResponse antwort = statuswechselService.wechsleStatus(antragId,
                new StatuswechselRequest(AntragStatus.IN_PRUEFUNG, "Unterlagen vollständig", "m.mueller"));

        assertThat(antwort.status()).isEqualTo(AntragStatus.IN_PRUEFUNG);
        assertThat(antrag.getStatus()).isEqualTo(AntragStatus.IN_PRUEFUNG);
        verify(auditService).protokolliere(antragId, AntragStatus.EINGEREICHT, AntragStatus.IN_PRUEFUNG,
                "Unterlagen vollständig", "m.mueller");
        // Für andere Status als BEWILLIGT wird keine Förderprüfung gebraucht.
        verifyNoInteractions(foerderpruefungService);
    }

    @Test
    void lehntVerbotenenUebergangAbOhneStatusZuAendernOderZuProtokollieren() {
        when(antragService.findeAntrag(antragId)).thenReturn(antrag);

        assertThatThrownBy(() -> statuswechselService.wechsleStatus(antragId,
                new StatuswechselRequest(AntragStatus.BEWILLIGT, null, "m.mueller")))
                .isInstanceOf(UngueltigerStatuswechselException.class)
                .hasMessage("Ein Wechsel von EINGEREICHT nach BEWILLIGT ist nicht erlaubt.");

        assertThat(antrag.getStatus()).isEqualTo(AntragStatus.EINGEREICHT);
        verifyNoInteractions(auditService);
    }

    @Test
    void meldetFinalenStatusMitEigenerFehlermeldung() {
        antrag.wechsleStatus(AntragStatus.ZURUECKGEZOGEN);
        when(antragService.findeAntrag(antragId)).thenReturn(antrag);

        assertThatThrownBy(() -> statuswechselService.wechsleStatus(antragId,
                new StatuswechselRequest(AntragStatus.IN_PRUEFUNG, null, "m.mueller")))
                .isInstanceOf(UngueltigerStatuswechselException.class)
                .hasMessageContaining("finalen Status ZURUECKGEZOGEN");
    }

    @Test
    void bewilligtNurWennAntragFoerderfaehigIst() {
        antrag.wechsleStatus(AntragStatus.IN_PRUEFUNG);
        when(antragService.findeAntrag(antragId)).thenReturn(antrag);
        when(foerderpruefungService.pruefe(antrag)).thenReturn(new PruefungsErgebnis(antragId, true,
                List.of(RegelErgebnis.erfuellt("Kreditbetrag", "ok"))));

        AntragResponse antwort = statuswechselService.wechsleStatus(antragId,
                new StatuswechselRequest(AntragStatus.BEWILLIGT, "Alles in Ordnung", "m.mueller"));

        assertThat(antwort.status()).isEqualTo(AntragStatus.BEWILLIGT);
        verify(auditService).protokolliere(antragId, AntragStatus.IN_PRUEFUNG, AntragStatus.BEWILLIGT,
                "Alles in Ordnung", "m.mueller");
    }

    @Test
    void lehntBewilligungAbWennAntragNichtFoerderfaehigIst() {
        antrag.wechsleStatus(AntragStatus.IN_PRUEFUNG);
        when(antragService.findeAntrag(antragId)).thenReturn(antrag);
        when(foerderpruefungService.pruefe(antrag)).thenReturn(new PruefungsErgebnis(antragId, false, List.of(
                RegelErgebnis.erfuellt("Kreditbetrag", "ok"),
                RegelErgebnis.nichtErfuellt("Standort", "Der Sitz liegt in BAYERN."))));

        assertThatThrownBy(() -> statuswechselService.wechsleStatus(antragId,
                new StatuswechselRequest(AntragStatus.BEWILLIGT, null, "m.mueller")))
                .isInstanceOfSatisfying(NichtFoerderfaehigException.class, ex ->
                        assertThat(ex.getNichtErfuellteRegeln())
                                .extracting(RegelErgebnis::regelName)
                                .containsExactly("Standort"));

        assertThat(antrag.getStatus()).isEqualTo(AntragStatus.IN_PRUEFUNG);
        verify(auditService, never()).protokolliere(any(), any(), any(), any(), anyString());
    }

    @Test
    void reichtNichtGefundenWeiterWennAntragUnbekanntIst() {
        when(antragService.findeAntrag(antragId)).thenThrow(new AntragNichtGefundenException(antragId));

        assertThatThrownBy(() -> statuswechselService.wechsleStatus(antragId,
                new StatuswechselRequest(AntragStatus.IN_PRUEFUNG, null, "m.mueller")))
                .isInstanceOf(AntragNichtGefundenException.class);
        verifyNoInteractions(auditService);
    }
}
