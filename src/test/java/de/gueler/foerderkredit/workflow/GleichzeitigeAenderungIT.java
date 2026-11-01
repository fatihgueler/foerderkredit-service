package de.gueler.foerderkredit.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gueler.foerderkredit.antrag.Antrag;
import de.gueler.foerderkredit.antrag.AntragRepository;
import de.gueler.foerderkredit.antrag.AntragService;
import de.gueler.foerderkredit.antrag.AntragStatus;
import de.gueler.foerderkredit.antrag.AntragTestdaten;
import de.gueler.foerderkredit.audit.AuditEintragResponse;
import de.gueler.foerderkredit.audit.AuditService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spielt nach, dass zwei Sachbearbeiter denselben Antrag gleichzeitig ändern.
 *
 * <p>Der Ablauf ist deterministisch, ganz ohne Threads und Wartezeiten:
 * <ol>
 *   <li>Bearbeiterin A öffnet eine Transaktion und lädt den Antrag (Version 0, EINGEREICHT).</li>
 *   <li>Während A's Transaktion noch offen ist, läuft Bearbeiter B's Statuswechsel in einer
 *       eigenen, NEUEN Transaktion komplett durch und committet (Version 1, IN_PRUEFUNG).</li>
 *   <li>A arbeitet mit ihrem veralteten Stand weiter (sieht noch EINGEREICHT) und will den Antrag
 *       zurückziehen. Beim Commit merkt Hibernate: Version in der DB ist 1, nicht 0.</li>
 * </ol>
 * Erwartung: A scheitert, B's Änderung bleibt erhalten, A's Audit-Eintrag wird mit zurückgerollt.
 *
 * <p>Ohne laufendes Docker wird der Test übersprungen (nicht rot), siehe README.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class GleichzeitigeAenderungIT {

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
    @Autowired
    private AntragService antragService;
    @Autowired
    private StatuswechselService statuswechselService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void zweiterGleichzeitigerStatuswechselScheitertUndUeberschreibtNichts() {
        UUID id = antragRepository.save(AntragTestdaten.gueltig().baue()).getId();

        TransactionTemplate transaktionVonA = new TransactionTemplate(transactionManager);
        TransactionTemplate neueTransaktionVonB = new TransactionTemplate(transactionManager);
        neueTransaktionVonB.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> transaktionVonA.executeWithoutResult(statusVonA -> {
            // A lädt den Antrag: Version 0, Status EINGEREICHT.
            Antrag standVonA = antragService.findeAntrag(id);
            assertThat(standVonA.getVersion()).isZero();

            // B ist schneller und committet in einer eigenen Transaktion.
            neueTransaktionVonB.executeWithoutResult(statusVonB ->
                    statuswechselService.wechsleStatus(id,
                            new StatuswechselRequest(AntragStatus.IN_PRUEFUNG, "B war schneller", "bearbeiter.b")));

            // A sieht noch ihren alten Stand (EINGEREICHT) und zieht den Antrag zurück.
            statuswechselService.wechsleStatus(id,
                    new StatuswechselRequest(AntragStatus.ZURUECKGEZOGEN, "A mit altem Stand", "bearbeiterin.a"));
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // B's Änderung ist erhalten, A hat nichts überschrieben.
        Antrag inDerDatenbank = antragRepository.findById(id).orElseThrow();
        assertThat(inDerDatenbank.getStatus()).isEqualTo(AntragStatus.IN_PRUEFUNG);
        assertThat(inDerDatenbank.getVersion()).isEqualTo(1);

        // Nur B's Audit-Eintrag existiert; A's Eintrag wurde mit zurückgerollt.
        List<AuditEintragResponse> historie = auditService.historie(id);
        assertThat(historie).hasSize(1);
        assertThat(historie.get(0).bearbeiter()).isEqualTo("bearbeiter.b");
    }

    @Test
    void erhoehtDieVersionBeiJedemStatuswechsel() {
        UUID id = antragRepository.save(AntragTestdaten.gueltig().baue()).getId();

        statuswechselService.wechsleStatus(id, new StatuswechselRequest(AntragStatus.IN_PRUEFUNG, null, "m.mueller"));
        statuswechselService.wechsleStatus(id, new StatuswechselRequest(AntragStatus.RUECKFRAGE, null, "m.mueller"));

        assertThat(antragRepository.findById(id).orElseThrow().getVersion()).isEqualTo(2);
    }
}
