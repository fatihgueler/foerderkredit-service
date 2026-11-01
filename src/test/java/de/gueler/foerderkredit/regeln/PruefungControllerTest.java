package de.gueler.foerderkredit.regeln;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gueler.foerderkredit.antrag.AntragNichtGefundenException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PruefungController.class)
class PruefungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FoerderpruefungService foerderpruefungService;

    @Test
    void liefertAlleRegelergebnisseUndGesamtergebnis() throws Exception {
        UUID id = UUID.randomUUID();
        when(foerderpruefungService.pruefe(id)).thenReturn(new PruefungsErgebnis(id, false, List.of(
                RegelErgebnis.erfuellt("Branche", "ok"),
                RegelErgebnis.nichtErfuellt("Standort", "Der Sitz liegt in BAYERN."))));

        mockMvc.perform(post("/api/antraege/{id}/pruefung", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.antragId").value(id.toString()))
                .andExpect(jsonPath("$.foerderfaehig").value(false))
                .andExpect(jsonPath("$.regelergebnisse.length()").value(2))
                .andExpect(jsonPath("$.regelergebnisse[1].regelName").value("Standort"))
                .andExpect(jsonPath("$.regelergebnisse[1].erfuellt").value(false))
                .andExpect(jsonPath("$.regelergebnisse[1].begruendung").value("Der Sitz liegt in BAYERN."));
    }

    @Test
    void liefert404WennAntragNichtExistiert() throws Exception {
        UUID id = UUID.randomUUID();
        when(foerderpruefungService.pruefe(id)).thenThrow(new AntragNichtGefundenException(id));

        mockMvc.perform(post("/api/antraege/{id}/pruefung", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Antrag nicht gefunden"));
    }
}
