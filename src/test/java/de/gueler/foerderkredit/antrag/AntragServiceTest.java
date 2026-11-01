package de.gueler.foerderkredit.antrag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Unit-Test ohne Spring und ohne Datenbank: das Repository ist ein Mockito-Mock. */
@ExtendWith(MockitoExtension.class)
class AntragServiceTest {

    @Mock
    private AntragRepository antragRepository;

    @InjectMocks
    private AntragService antragService;

    @Test
    void legtNeuenAntragImStatusEingereichtAn() {
        // Das Repository gibt einfach das übergebene Objekt zurück.
        when(antragRepository.save(any(Antrag.class))).thenAnswer(aufruf -> aufruf.getArgument(0));

        AntragResponse antwort = antragService.anlegen(AntragTestdaten.gueltig().baueRequest());

        assertThat(antwort.status()).isEqualTo(AntragStatus.EINGEREICHT);
        assertThat(antwort.kreditbetrag()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(antwort.erstelltAm()).isNotNull();
        assertThat(antwort.geaendertAm()).isEqualTo(antwort.erstelltAm());
    }

    @Test
    void wirftNichtGefundenWennIdUnbekanntIst() {
        UUID unbekannt = UUID.randomUUID();
        when(antragRepository.findById(unbekannt)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> antragService.lade(unbekannt))
                .isInstanceOf(AntragNichtGefundenException.class)
                .hasMessageContaining(unbekannt.toString());
    }

    @Test
    void suchtNurNachStatusWennStatusAngegebenIst() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Antrag> seite = new PageImpl<>(List.of(AntragTestdaten.gueltig().baue()), pageable, 1);
        when(antragRepository.findByStatus(AntragStatus.EINGEREICHT, pageable)).thenReturn(seite);

        AntragSeiteResponse antwort = antragService.suche(AntragStatus.EINGEREICHT, pageable);

        assertThat(antwort.inhalt()).hasSize(1);
        assertThat(antwort.gesamtAnzahl()).isEqualTo(1);
        verify(antragRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void liefertAlleAntraegeWennKeinStatusAngegebenIst() {
        Pageable pageable = PageRequest.of(0, 20);
        when(antragRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        AntragSeiteResponse antwort = antragService.suche(null, pageable);

        assertThat(antwort.inhalt()).isEmpty();
        verify(antragRepository, never()).findByStatus(any(), any());
    }
}
