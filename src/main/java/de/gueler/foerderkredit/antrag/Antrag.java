package de.gueler.foerderkredit.antrag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Ein Förderkredit-Antrag, so wie er in der Datenbank steht.
 *
 * <p>Diese Klasse verlässt nie den Server: Nach außen gehen nur DTOs
 * (z. B. {@link AntragResponse}). So kann sich das Datenbankmodell ändern,
 * ohne dass sich die REST-API ändert.
 *
 * <p>Es gibt bewusst keine Setter für die Antragsdaten. Der einzige Weg, den
 * Zustand zu ändern, ist {@link #wechsleStatus(AntragStatus)}.
 */
@Entity
@Table(name = "antrag")
public class Antrag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "antragsteller_name", nullable = false, length = 200)
    private String antragstellerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rechtsform", nullable = false, length = 30)
    private Rechtsform rechtsform;

    @Enumerated(EnumType.STRING)
    @Column(name = "branche", nullable = false, length = 30)
    private Branche branche;

    @Enumerated(EnumType.STRING)
    @Column(name = "bundesland", nullable = false, length = 30)
    private Bundesland bundesland;

    @Column(name = "mitarbeiter_anzahl", nullable = false)
    private int mitarbeiterAnzahl;

    @Column(name = "jahresumsatz", nullable = false, precision = 15, scale = 2)
    private BigDecimal jahresumsatz;

    @Column(name = "kreditbetrag", nullable = false, precision = 15, scale = 2)
    private BigDecimal kreditbetrag;

    @Column(name = "laufzeit_jahre", nullable = false)
    private int laufzeitJahre;

    @Column(name = "tilgungsfreie_jahre", nullable = false)
    private int tilgungsfreieJahre;

    @Column(name = "sollzins_prozent", nullable = false, precision = 5, scale = 3)
    private BigDecimal sollzinsProzent;

    @Column(name = "verwendungszweck", nullable = false, length = 500)
    private String verwendungszweck;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AntragStatus status;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private Instant erstelltAm;

    @Column(name = "geaendert_am", nullable = false)
    private Instant geaendertAm;

    /** Nur für JPA/Hibernate. Hibernate braucht einen parameterlosen Konstruktor. */
    protected Antrag() {
    }

    /** Legt einen neuen Antrag im Status {@link AntragStatus#EINGEREICHT} an. */
    public Antrag(String antragstellerName,
                  Rechtsform rechtsform,
                  Branche branche,
                  Bundesland bundesland,
                  int mitarbeiterAnzahl,
                  BigDecimal jahresumsatz,
                  BigDecimal kreditbetrag,
                  int laufzeitJahre,
                  int tilgungsfreieJahre,
                  BigDecimal sollzinsProzent,
                  String verwendungszweck) {
        this.antragstellerName = antragstellerName;
        this.rechtsform = rechtsform;
        this.branche = branche;
        this.bundesland = bundesland;
        this.mitarbeiterAnzahl = mitarbeiterAnzahl;
        // setScale ohne RoundingMode: Hat der Betrag mehr als 2 Nachkommastellen, gibt es
        // eine ArithmeticException statt einer stillen Rundung. Die Validierung im Request
        // lässt aber ohnehin höchstens 2 Nachkommastellen zu.
        this.jahresumsatz = jahresumsatz.setScale(2);
        this.kreditbetrag = kreditbetrag.setScale(2);
        this.laufzeitJahre = laufzeitJahre;
        this.tilgungsfreieJahre = tilgungsfreieJahre;
        this.sollzinsProzent = sollzinsProzent.setScale(3);
        this.verwendungszweck = verwendungszweck;
        this.status = AntragStatus.EINGEREICHT;
        this.erstelltAm = jetzt();
        this.geaendertAm = this.erstelltAm;
    }

    /**
     * Setzt den neuen Status und aktualisiert den Änderungszeitpunkt.
     * Ob der Wechsel erlaubt ist, prüft der Aufrufer (Workflow) vorher.
     */
    public void wechsleStatus(AntragStatus neuerStatus) {
        this.status = neuerStatus;
        this.geaendertAm = jetzt();
    }

    /** PostgreSQL speichert Zeitstempel auf Mikrosekunden genau, daher schneiden wir hier ab. */
    private static Instant jetzt() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public UUID getId() {
        return id;
    }

    public String getAntragstellerName() {
        return antragstellerName;
    }

    public Rechtsform getRechtsform() {
        return rechtsform;
    }

    public Branche getBranche() {
        return branche;
    }

    public Bundesland getBundesland() {
        return bundesland;
    }

    public int getMitarbeiterAnzahl() {
        return mitarbeiterAnzahl;
    }

    public BigDecimal getJahresumsatz() {
        return jahresumsatz;
    }

    public BigDecimal getKreditbetrag() {
        return kreditbetrag;
    }

    public int getLaufzeitJahre() {
        return laufzeitJahre;
    }

    public int getTilgungsfreieJahre() {
        return tilgungsfreieJahre;
    }

    public BigDecimal getSollzinsProzent() {
        return sollzinsProzent;
    }

    public String getVerwendungszweck() {
        return verwendungszweck;
    }

    public AntragStatus getStatus() {
        return status;
    }

    public Instant getErstelltAm() {
        return erstelltAm;
    }

    public Instant getGeaendertAm() {
        return geaendertAm;
    }
}
