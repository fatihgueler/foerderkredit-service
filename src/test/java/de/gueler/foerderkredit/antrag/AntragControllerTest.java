package de.gueler.foerderkredit.antrag;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testet nur die Web-Schicht: Controller, JSON-Umwandlung, Validierung und
 * Fehlerbehandlung. {@code @WebMvcTest} startet keinen Server und keine Datenbank;
 * der Service ist ein Mock.
 */
@WebMvcTest(AntragController.class)
class AntragControllerTest {

    private static final String GUELTIGER_ANTRAG = """
            {
              "antragstellerName": "Muster Tischlerei GmbH",
              "rechtsform": "GMBH",
              "branche": "HANDWERK",
              "bundesland": "NIEDERSACHSEN",
              "mitarbeiterAnzahl": 25,
              "jahresumsatz": 3500000.00,
              "kreditbetrag": 100000.00,
              "laufzeitJahre": 10,
              "tilgungsfreieJahre": 1,
              "sollzinsProzent": 4.0,
              "verwendungszweck": "Kauf einer CNC-Fräsmaschine"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AntragService antragService;

    @Test
    void legtGueltigenAntragAnUndAntwortetMit201UndLocationHeader() throws Exception {
        UUID id = UUID.randomUUID();
        when(antragService.anlegen(any())).thenReturn(beispielAntwort(id));

        mockMvc.perform(post("/api/antraege")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_ANTRAG))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/antraege/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("EINGEREICHT"));
    }

    @Test
    void liefert400MitDeutscherMeldungJeFeldWennPflichtfelderFehlen() throws Exception {
        mockMvc.perform(post("/api/antraege")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Ungültige Eingabe"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fehler.antragstellerName")
                        .value("Bitte den Namen des Antragstellers angeben."))
                .andExpect(jsonPath("$.fehler.kreditbetrag").value("Bitte den Kreditbetrag angeben."))
                .andExpect(jsonPath("$.fehler.bundesland").value("Bitte das Bundesland angeben."))
                .andExpect(jsonPath("$.fehler.verwendungszweck").value("Bitte den Verwendungszweck angeben."));

        // Bei ungültiger Eingabe darf der Service gar nicht erst aufgerufen werden.
        verifyNoInteractions(antragService);
    }

    @Test
    void liefert400WennVerwendungszweckLaengerAls500ZeichenIst() throws Exception {
        String zuLang = "x".repeat(501);
        String json = GUELTIGER_ANTRAG.replace("Kauf einer CNC-Fräsmaschine", zuLang);

        mockMvc.perform(post("/api/antraege")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fehler.verwendungszweck")
                        .value("Der Verwendungszweck darf höchstens 500 Zeichen lang sein."));
    }

    @Test
    void liefert400WennKreditbetragNegativIst() throws Exception {
        String json = GUELTIGER_ANTRAG.replace("\"kreditbetrag\": 100000.00", "\"kreditbetrag\": -1");

        mockMvc.perform(post("/api/antraege")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fehler.kreditbetrag").value("Der Kreditbetrag muss größer als 0 sein."));
    }

    @Test
    void liefert400MitErlaubtenWertenWennBrancheUnbekanntIst() throws Exception {
        String json = GUELTIGER_ANTRAG.replace("\"HANDWERK\"", "\"RAUMFAHRT\"");

        mockMvc.perform(post("/api/antraege")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Ungültiger Wert 'RAUMFAHRT' für das Feld 'branche'")))
                .andExpect(jsonPath("$.detail", containsString("HANDWERK")));
    }

    @Test
    void liefert400WennBodyKeinGueltigesJsonIst() throws Exception {
        mockMvc.perform(post("/api/antraege")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ das ist kein json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ungültige Eingabe"));
    }

    @Test
    void liefertAntragZuBekannterId() throws Exception {
        UUID id = UUID.randomUUID();
        when(antragService.lade(id)).thenReturn(beispielAntwort(id));

        mockMvc.perform(get("/api/antraege/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.antragstellerName").value("Muster Tischlerei GmbH"))
                .andExpect(jsonPath("$.kreditbetrag").value(100000.00));
    }

    @Test
    void liefert404MitProblemDetailWennAntragNichtExistiert() throws Exception {
        UUID id = UUID.randomUUID();
        when(antragService.lade(id)).thenThrow(new AntragNichtGefundenException(id));

        mockMvc.perform(get("/api/antraege/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Antrag nicht gefunden"))
                .andExpect(jsonPath("$.detail").value("Es gibt keinen Antrag mit der ID " + id + "."));
    }

    @Test
    void liefert400WennIdKeineUuidIst() throws Exception {
        mockMvc.perform(get("/api/antraege/{id}", "keine-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Ungültiger Wert 'keine-uuid' für 'id'."));
    }

    @Test
    void reichtStatusfilterUndSeiteAnDenServiceWeiter() throws Exception {
        UUID id = UUID.randomUUID();
        when(antragService.suche(eq(AntragStatus.IN_PRUEFUNG), any(Pageable.class)))
                .thenReturn(new AntragSeiteResponse(List.of(beispielAntwort(id)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/antraege").param("status", "IN_PRUEFUNG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inhalt[0].id").value(id.toString()))
                .andExpect(jsonPath("$.gesamtAnzahl").value(1));

        verify(antragService).suche(eq(AntragStatus.IN_PRUEFUNG), any(Pageable.class));
    }

    @Test
    void liefert400WennStatusfilterUnbekanntIst() throws Exception {
        mockMvc.perform(get("/api/antraege").param("status", "VERLOREN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Erlaubte Werte: EINGEREICHT")));
    }

    private static AntragResponse beispielAntwort(UUID id) {
        Instant jetzt = Instant.parse("2026-11-01T10:00:00Z");
        return new AntragResponse(id, "Muster Tischlerei GmbH", Rechtsform.GMBH, Branche.HANDWERK,
                Bundesland.NIEDERSACHSEN, 25, new BigDecimal("3500000.00"), new BigDecimal("100000.00"),
                10, 1, new BigDecimal("4.000"), "Kauf einer CNC-Fräsmaschine", AntragStatus.EINGEREICHT,
                jetzt, jetzt);
    }
}
