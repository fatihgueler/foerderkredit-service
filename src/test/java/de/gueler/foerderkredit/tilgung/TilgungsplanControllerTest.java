package de.gueler.foerderkredit.tilgung;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gueler.foerderkredit.antrag.AntragNichtGefundenException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TilgungsplanController.class)
class TilgungsplanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TilgungsplanService tilgungsplanService;

    @Test
    void liefertRatenUndSummenAlsJson() throws Exception {
        UUID id = UUID.randomUUID();
        Rate rate = new Rate(1, LocalDate.of(2027, 1, 1), new BigDecimal("333.33"),
                new BigDecimal("0.00"), new BigDecimal("333.33"), new BigDecimal("100000.00"));
        when(tilgungsplanService.berechne(id))
                .thenReturn(new Tilgungsplan(List.of(rate), new BigDecimal("333.33"), new BigDecimal("0.00")));

        mockMvc.perform(get("/api/antraege/{id}/tilgungsplan", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raten[0].monat").value(1))
                .andExpect(jsonPath("$.raten[0].datum").value("2027-01-01"))
                .andExpect(jsonPath("$.raten[0].zinsanteil").value(333.33))
                .andExpect(jsonPath("$.gesamtZinsen").value(333.33));
    }

    @Test
    void liefert404WennAntragNichtExistiert() throws Exception {
        UUID id = UUID.randomUUID();
        when(tilgungsplanService.berechne(id)).thenThrow(new AntragNichtGefundenException(id));

        mockMvc.perform(get("/api/antraege/{id}/tilgungsplan", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void liefert422WennTilgungsplanNichtBerechenbarIst() throws Exception {
        UUID id = UUID.randomUUID();
        when(tilgungsplanService.berechne(id))
                .thenThrow(new TilgungsplanNichtBerechenbarException("Die tilgungsfreien Jahre ..."));

        mockMvc.perform(get("/api/antraege/{id}/tilgungsplan", id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Tilgungsplan nicht berechenbar"));
    }
}
