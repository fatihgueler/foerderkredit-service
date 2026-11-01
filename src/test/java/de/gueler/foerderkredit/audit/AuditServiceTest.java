package de.gueler.foerderkredit.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gueler.foerderkredit.antrag.AntragNichtGefundenException;
import de.gueler.foerderkredit.antrag.AntragService;
import de.gueler.foerderkredit.antrag.AntragStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;
    @Mock
    private AntragService antragService;

    @InjectMocks
    private AuditService auditService;

    private final UUID antragId = UUID.randomUUID();

    @Test
    void speichertEintragMitAllenAngabenUndZeitpunkt() {
        auditService.protokolliere(antragId, AntragStatus.EINGEREICHT, AntragStatus.IN_PRUEFUNG,
                "Start", "m.mueller");

        ArgumentCaptor<AuditEintrag> captor = ArgumentCaptor.forClass(AuditEintrag.class);
        verify(auditRepository).save(captor.capture());
        AuditEintrag eintrag = captor.getValue();
        assertThat(eintrag.getAntragId()).isEqualTo(antragId);
        assertThat(eintrag.getVonStatus()).isEqualTo(AntragStatus.EINGEREICHT);
        assertThat(eintrag.getNachStatus()).isEqualTo(AntragStatus.IN_PRUEFUNG);
        assertThat(eintrag.getKommentar()).isEqualTo("Start");
        assertThat(eintrag.getBearbeiter()).isEqualTo("m.mueller");
        assertThat(eintrag.getZeitpunkt()).isNotNull();
    }

    @Test
    void liefertHistorieAlsDtoInDerReihenfolgeDesRepositorys() {
        AuditEintrag erster = new AuditEintrag(antragId, AntragStatus.EINGEREICHT, AntragStatus.IN_PRUEFUNG, null, "a");
        AuditEintrag zweiter = new AuditEintrag(antragId, AntragStatus.IN_PRUEFUNG, AntragStatus.BEWILLIGT, null, "b");
        when(auditRepository.findByAntragIdOrderByZeitpunktAsc(antragId)).thenReturn(List.of(erster, zweiter));

        List<AuditEintragResponse> historie = auditService.historie(antragId);

        assertThat(historie).extracting(AuditEintragResponse::nachStatus)
                .containsExactly(AntragStatus.IN_PRUEFUNG, AntragStatus.BEWILLIGT);
    }

    @Test
    void liefertNichtGefundenStattLeererHistorieBeiUnbekanntemAntrag() {
        when(antragService.findeAntrag(antragId)).thenThrow(new AntragNichtGefundenException(antragId));

        assertThatThrownBy(() -> auditService.historie(antragId))
                .isInstanceOf(AntragNichtGefundenException.class);
        verifyNoInteractions(auditRepository);
    }
}
