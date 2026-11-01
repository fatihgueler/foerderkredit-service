package de.gueler.foerderkredit.workflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gueler.foerderkredit.antrag.AntragNichtGefundenException;
import de.gueler.foerderkredit.antrag.AntragResponse;
import de.gueler.foerderkredit.antrag.AntragStatus;
import de.gueler.foerderkredit.antrag.Branche;
import de.gueler.foerderkredit.antrag.Bundesland;
import de.gueler.foerderkredit.antrag.Rechtsform;
import de.gueler.foerderkredit.regeln.PruefungsErgebnis;
import de.gueler.foerderkredit.regeln.RegelErgebnis;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StatuswechselController.class)
class StatuswechselControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatuswechselService statuswechselService;

    private final UUID id = UUID.randomUUID();

    @Test
    void liefertAktualisiertenAntragNachErlaubtemWechsel() throws Exception {
        when(statuswechselService.wechsleStatus(eq(id), any())).thenReturn(antragImStatus(AntragStatus.IN_PRUEFUNG));

        mockMvc.perform(post("/api/antraege/{id}/statuswechsel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"neuerStatus": "IN_PRUEFUNG", "kommentar": "Start", "bearbeiter": "m.mueller"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PRUEFUNG"));
    }

    @Test
    void liefert400WennBearbeiterUndNeuerStatusFehlen() throws Exception {
        mockMvc.perform(post("/api/antraege/{id}/statuswechsel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kommentar\": \"ohne Pflichtfelder\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fehler.neuerStatus").value("Bitte den neuen Status angeben."))
                .andExpect(jsonPath("$.fehler.bearbeiter").value("Bitte den Bearbeiter angeben."));

        verifyNoInteractions(statuswechselService);
    }

    @Test
    void liefert404WennAntragNichtExistiert() throws Exception {
        when(statuswechselService.wechsleStatus(eq(id), any())).thenThrow(new AntragNichtGefundenException(id));

        mockMvc.perform(post("/api/antraege/{id}/statuswechsel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"neuerStatus\": \"IN_PRUEFUNG\", \"bearbeiter\": \"m.mueller\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void liefert409MitErlaubtenFolgestatusBeiVerbotenemUebergang() throws Exception {
        when(statuswechselService.wechsleStatus(eq(id), any()))
                .thenThrow(new UngueltigerStatuswechselException(AntragStatus.EINGEREICHT, AntragStatus.BEWILLIGT));

        mockMvc.perform(post("/api/antraege/{id}/statuswechsel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"neuerStatus\": \"BEWILLIGT\", \"bearbeiter\": \"m.mueller\"}"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Statuswechsel nicht erlaubt"))
                .andExpect(jsonPath("$.detail").value("Ein Wechsel von EINGEREICHT nach BEWILLIGT ist nicht erlaubt."))
                .andExpect(jsonPath("$.aktuellerStatus").value("EINGEREICHT"))
                .andExpect(jsonPath("$.erlaubteFolgestatus[0]").value("IN_PRUEFUNG"))
                .andExpect(jsonPath("$.erlaubteFolgestatus[1]").value("ZURUECKGEZOGEN"));
    }

    @Test
    void liefert422MitNichtErfuelltenRegelnWennNichtFoerderfaehig() throws Exception {
        PruefungsErgebnis pruefung = new PruefungsErgebnis(id, false, List.of(
                RegelErgebnis.erfuellt("Kreditbetrag", "ok"),
                RegelErgebnis.nichtErfuellt("Standort", "Der Sitz liegt in BAYERN.")));
        when(statuswechselService.wechsleStatus(eq(id), any())).thenThrow(new NichtFoerderfaehigException(pruefung));

        mockMvc.perform(post("/api/antraege/{id}/statuswechsel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"neuerStatus\": \"BEWILLIGT\", \"bearbeiter\": \"m.mueller\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Antrag nicht förderfähig"))
                .andExpect(jsonPath("$.nichtErfuellteRegeln.length()").value(1))
                .andExpect(jsonPath("$.nichtErfuellteRegeln[0].regelName").value("Standort"));
    }

    @Test
    void liefert409WennJemandAnderesDenAntragGleichzeitigGeaendertHat() throws Exception {
        when(statuswechselService.wechsleStatus(eq(id), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("Antrag", id));

        mockMvc.perform(post("/api/antraege/{id}/statuswechsel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"neuerStatus\": \"IN_PRUEFUNG\", \"bearbeiter\": \"m.mueller\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Gleichzeitige Änderung"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("zwischenzeitlich")));
    }

    private AntragResponse antragImStatus(AntragStatus status) {
        Instant jetzt = Instant.parse("2026-11-01T10:00:00Z");
        return new AntragResponse(id, "Muster Tischlerei GmbH", Rechtsform.GMBH, Branche.HANDWERK,
                Bundesland.NIEDERSACHSEN, 25, new BigDecimal("3500000.00"), new BigDecimal("100000.00"),
                10, 1, new BigDecimal("4.000"), "Kauf einer CNC-Fräsmaschine", status, jetzt, jetzt);
    }
}
