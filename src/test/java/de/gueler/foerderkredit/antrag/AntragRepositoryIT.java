package de.gueler.foerderkredit.antrag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prüft das Zusammenspiel von Flyway-Migration, JPA-Mapping und echter PostgreSQL.
 * Schon das Starten beweist, dass {@code ddl-auto=validate} zum Flyway-Schema passt.
 *
 * <p>Ohne laufendes Docker wird der Test übersprungen (nicht rot), siehe README.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class AntragRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datenbankEinstellungen(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AntragRepository antragRepository;

    @Test
    void speichertAntragUndLiestIhnUnveraendertWieder() {
        Antrag gespeichert = antragRepository.saveAndFlush(AntragTestdaten.gueltig().baue());

        Antrag geladen = antragRepository.findById(gespeichert.getId()).orElseThrow();

        assertThat(geladen.getId()).isNotNull();
        assertThat(geladen.getKreditbetrag()).isEqualByComparingTo("100000.00");
        assertThat(geladen.getBundesland()).isEqualTo(Bundesland.NIEDERSACHSEN);
        assertThat(geladen.getStatus()).isEqualTo(AntragStatus.EINGEREICHT);
    }

    @Test
    void findetNurAntraegeMitGesuchtemStatus() {
        antragRepository.save(AntragTestdaten.gueltig().baue());
        Antrag inPruefung = AntragTestdaten.gueltig().baue();
        inPruefung.wechsleStatus(AntragStatus.IN_PRUEFUNG);
        antragRepository.saveAndFlush(inPruefung);

        Page<Antrag> treffer = antragRepository.findByStatus(AntragStatus.IN_PRUEFUNG, PageRequest.of(0, 10));

        assertThat(treffer.getContent())
                .extracting(Antrag::getStatus)
                .containsOnly(AntragStatus.IN_PRUEFUNG);
    }
}
