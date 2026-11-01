package de.gueler.foerderkredit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integrationstest über die ganze Anwendung: echte HTTP-Verarbeitung (MockMvc), echte Services,
 * echte Transaktionen, Flyway-Migrationen und eine echte PostgreSQL 16 im Docker-Container.
 *
 * <p>Ohne laufendes Docker wird der Test übersprungen (nicht rot). In GitHub Actions läuft er.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AntragLebenszyklusIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datenbankEinstellungen(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final String ANTRAG_NIEDERSACHSEN = """
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

    @Test
    void antragAnlegenPruefenUndBewilligenErzeugtZweiHistorieneintraege() throws Exception {
        // 1. Antrag anlegen
        String id = legeAntragAn(ANTRAG_NIEDERSACHSEN);

        // 2. Prüfen: alle fiktiven Regeln erfüllt
        mockMvc.perform(post("/api/antraege/{id}/pruefung", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foerderfaehig").value(true))
                .andExpect(jsonPath("$.regelergebnisse.length()").value(5));

        // 3. In Prüfung nehmen und bewilligen
        wechsleStatus(id, "IN_PRUEFUNG")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PRUEFUNG"));
        wechsleStatus(id, "BEWILLIGT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BEWILLIGT"));

        // 4. Historie enthält genau die zwei Statuswechsel in der richtigen Reihenfolge
        mockMvc.perform(get("/api/antraege/{id}/historie", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].vonStatus").value("EINGEREICHT"))
                .andExpect(jsonPath("$[0].nachStatus").value("IN_PRUEFUNG"))
                .andExpect(jsonPath("$[1].vonStatus").value("IN_PRUEFUNG"))
                .andExpect(jsonPath("$[1].nachStatus").value("BEWILLIGT"))
                .andExpect(jsonPath("$[1].bearbeiter").value("m.mueller"));

        // 5. Ein finaler Antrag lässt sich nicht mehr ändern
        wechsleStatus(id, "IN_PRUEFUNG").andExpect(status().isConflict());

        // 6. Der Antrag ist in der Liste der bewilligten Anträge zu finden
        mockMvc.perform(get("/api/antraege").param("status", "BEWILLIGT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inhalt[?(@.id == '" + id + "')]").exists());
    }

    @Test
    void abgelehnteBewilligungAendertWederStatusNochHistorie() throws Exception {
        // Sitz in Bayern: die fiktive Standortregel ist nicht erfüllt.
        String id = legeAntragAn(ANTRAG_NIEDERSACHSEN.replace("NIEDERSACHSEN", "BAYERN"));
        wechsleStatus(id, "IN_PRUEFUNG").andExpect(status().isOk());

        wechsleStatus(id, "BEWILLIGT")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.nichtErfuellteRegeln[0].regelName").value("Standort"));

        // Rollback: Status ist unverändert, es gibt keinen zweiten Audit-Eintrag.
        mockMvc.perform(get("/api/antraege/{id}", id))
                .andExpect(jsonPath("$.status").value("IN_PRUEFUNG"));
        mockMvc.perform(get("/api/antraege/{id}/historie", id))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void liefertTilgungsplanFuerGespeichertenAntrag() throws Exception {
        String id = legeAntragAn(ANTRAG_NIEDERSACHSEN);

        mockMvc.perform(get("/api/antraege/{id}/tilgungsplan", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raten.length()").value(120))
                .andExpect(jsonPath("$.raten[12].rate").value(1104.10))
                .andExpect(jsonPath("$.raten[119].restschuld").value(0.00))
                .andExpect(jsonPath("$.gesamtZinsen").value(23242.34));
    }

    private String legeAntragAn(String json) throws Exception {
        String antwort = mockMvc.perform(post("/api/antraege")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(antwort, "$.id");
        assertThat(id).isNotBlank();
        return id;
    }

    private ResultActions wechsleStatus(String id, String neuerStatus) throws Exception {
        return mockMvc.perform(post("/api/antraege/{id}/statuswechsel", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"neuerStatus": "%s", "kommentar": "Integrationstest", "bearbeiter": "m.mueller"}
                        """.formatted(neuerStatus)));
    }
}
