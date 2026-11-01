package de.gueler.foerderkredit.antrag;

import java.math.BigDecimal;

/**
 * Test-Data-Builder: erzeugt einen gültigen, förderfähigen Beispielantrag.
 * Ein Test ändert nur das eine Feld, um das es ihm geht, z. B.
 * {@code AntragTestdaten.gueltig().mitKreditbetrag("5000.00").baue()}.
 * So sieht man im Test sofort, worauf es ankommt.
 */
public final class AntragTestdaten {

    private String antragstellerName = "Muster Tischlerei GmbH";
    private Rechtsform rechtsform = Rechtsform.GMBH;
    private Branche branche = Branche.HANDWERK;
    private Bundesland bundesland = Bundesland.NIEDERSACHSEN;
    private int mitarbeiterAnzahl = 25;
    private BigDecimal jahresumsatz = new BigDecimal("3500000.00");
    private BigDecimal kreditbetrag = new BigDecimal("100000.00");
    private int laufzeitJahre = 10;
    private int tilgungsfreieJahre = 1;
    private BigDecimal sollzinsProzent = new BigDecimal("4.000");
    private String verwendungszweck = "Kauf einer CNC-Fräsmaschine";

    private AntragTestdaten() {
    }

    public static AntragTestdaten gueltig() {
        return new AntragTestdaten();
    }

    public AntragTestdaten mitBranche(Branche branche) {
        this.branche = branche;
        return this;
    }

    public AntragTestdaten mitBundesland(Bundesland bundesland) {
        this.bundesland = bundesland;
        return this;
    }

    public AntragTestdaten mitMitarbeiterAnzahl(int mitarbeiterAnzahl) {
        this.mitarbeiterAnzahl = mitarbeiterAnzahl;
        return this;
    }

    public AntragTestdaten mitJahresumsatz(String jahresumsatz) {
        this.jahresumsatz = new BigDecimal(jahresumsatz);
        return this;
    }

    public AntragTestdaten mitKreditbetrag(String kreditbetrag) {
        this.kreditbetrag = new BigDecimal(kreditbetrag);
        return this;
    }

    public AntragTestdaten mitLaufzeitJahre(int laufzeitJahre) {
        this.laufzeitJahre = laufzeitJahre;
        return this;
    }

    public AntragTestdaten mitTilgungsfreienJahren(int tilgungsfreieJahre) {
        this.tilgungsfreieJahre = tilgungsfreieJahre;
        return this;
    }

    public AntragTestdaten mitSollzinsProzent(String sollzinsProzent) {
        this.sollzinsProzent = new BigDecimal(sollzinsProzent);
        return this;
    }

    public Antrag baue() {
        return new Antrag(antragstellerName, rechtsform, branche, bundesland, mitarbeiterAnzahl,
                jahresumsatz, kreditbetrag, laufzeitJahre, tilgungsfreieJahre, sollzinsProzent,
                verwendungszweck);
    }

    /** Derselbe Antrag als Request-DTO, z. B. für Service-Tests. */
    public AntragAnlegenRequest baueRequest() {
        return new AntragAnlegenRequest(antragstellerName, rechtsform, branche, bundesland,
                mitarbeiterAnzahl, jahresumsatz, kreditbetrag, laufzeitJahre, tilgungsfreieJahre,
                sollzinsProzent, verwendungszweck);
    }
}
