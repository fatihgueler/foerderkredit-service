package de.gueler.foerderkredit.audit;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gueler.foerderkredit.antrag.AntragNichtGefundenException;
import de.gueler.foerderkredit.antrag.AntragStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HistorieController.class)
class HistorieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @Test
    void liefertAlleStatuswechselDesAntrags() throws Exception {
        UUID id = UUID.randomUUID();
        when(auditService.historie(id)).thenReturn(List.of(new AuditEintragResponse(UUID.randomUUID(), id,
                AntragStatus.EINGEREICHT, AntragStatus.IN_PRUEFUNG, "Start", "m.mueller",
                Instant.parse("2026-11-01T10:00:00Z"))));

        mockMvc.perform(get("/api/antraege/{id}/historie", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].vonStatus").value("EINGEREICHT"))
                .andExpect(jsonPath("$[0].nachStatus").value("IN_PRUEFUNG"))
                .andExpect(jsonPath("$[0].bearbeiter").value("m.mueller"))
                .andExpect(jsonPath("$[0].zeitpunkt").value("2026-11-01T10:00:00Z"));
    }

    @Test
    void liefert404WennAntragNichtExistiert() throws Exception {
        UUID id = UUID.randomUUID();
        when(auditService.historie(id)).thenThrow(new AntragNichtGefundenException(id));

        mockMvc.perform(get("/api/antraege/{id}/historie", id))
                .andExpect(status().isNotFound());
    }
}
