package de.gueler.foerderkredit.antrag;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Antwort-DTO für einen Antrag. Enthält nur Daten, die nach außen gehen dürfen. */
public record AntragResponse(
        UUID id,
        String antragstellerName,
        Rechtsform rechtsform,
        Branche branche,
        Bundesland bundesland,
        int mitarbeiterAnzahl,
        BigDecimal jahresumsatz,
        BigDecimal kreditbetrag,
        int laufzeitJahre,
        int tilgungsfreieJahre,
        BigDecimal sollzinsProzent,
        String verwendungszweck,
        AntragStatus status,
        Instant erstelltAm,
        Instant geaendertAm
) {

    /** Übersetzt die Entity in das DTO. */
    public static AntragResponse von(Antrag antrag) {
        return new AntragResponse(
                antrag.getId(),
                antrag.getAntragstellerName(),
                antrag.getRechtsform(),
                antrag.getBranche(),
                antrag.getBundesland(),
                antrag.getMitarbeiterAnzahl(),
                antrag.getJahresumsatz(),
                antrag.getKreditbetrag(),
                antrag.getLaufzeitJahre(),
                antrag.getTilgungsfreieJahre(),
                antrag.getSollzinsProzent(),
                antrag.getVerwendungszweck(),
                antrag.getStatus(),
                antrag.getErstelltAm(),
                antrag.getGeaendertAm());
    }
}
