package de.gueler.foerderkredit.audit;

import de.gueler.foerderkredit.antrag.AntragStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Ein Eintrag im Audit-Log: wer hat wann einen Antrag von welchem in welchen Status gesetzt.
 *
 * <p>Revisionssicher heißt hier: Ein Eintrag wird einmal geschrieben und danach nie geändert
 * oder gelöscht. Dafür sorgen drei Dinge:
 * <ul>
 *   <li>{@code @Immutable}: Hibernate schreibt Änderungen an diesem Objekt nie zurück.</li>
 *   <li>Keine Setter: Der Zustand wird nur im Konstruktor gesetzt.</li>
 *   <li>{@link AuditRepository} bietet nur {@code save} und Lesemethoden an, kein {@code delete}.</li>
 * </ul>
 */
@Entity
@Immutable
@Table(name = "audit_eintrag")
public class AuditEintrag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "antrag_id", nullable = false, updatable = false)
    private UUID antragId;

    @Enumerated(EnumType.STRING)
    @Column(name = "von_status", nullable = false, updatable = false, length = 30)
    private AntragStatus vonStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "nach_status", nullable = false, updatable = false, length = 30)
    private AntragStatus nachStatus;

    @Column(name = "kommentar", updatable = false, length = 500)
    private String kommentar;

    @Column(name = "bearbeiter", nullable = false, updatable = false, length = 100)
    private String bearbeiter;

    @Column(name = "zeitpunkt", nullable = false, updatable = false)
    private Instant zeitpunkt;

    /** Nur für JPA/Hibernate. */
    protected AuditEintrag() {
    }

    public AuditEintrag(UUID antragId, AntragStatus vonStatus, AntragStatus nachStatus,
                        String kommentar, String bearbeiter) {
        this.antragId = antragId;
        this.vonStatus = vonStatus;
        this.nachStatus = nachStatus;
        this.kommentar = kommentar;
        this.bearbeiter = bearbeiter;
        this.zeitpunkt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public UUID getId() {
        return id;
    }

    public UUID getAntragId() {
        return antragId;
    }

    public AntragStatus getVonStatus() {
        return vonStatus;
    }

    public AntragStatus getNachStatus() {
        return nachStatus;
    }

    public String getKommentar() {
        return kommentar;
    }

    public String getBearbeiter() {
        return bearbeiter;
    }

    public Instant getZeitpunkt() {
        return zeitpunkt;
    }
}
