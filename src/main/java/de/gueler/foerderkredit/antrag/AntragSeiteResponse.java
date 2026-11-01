package de.gueler.foerderkredit.antrag;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Eine Seite der Antragsliste. Eigenes DTO statt Springs {@code Page}-Objekt,
 * damit das JSON-Format stabil und selbst festgelegt ist.
 */
public record AntragSeiteResponse(
        List<AntragResponse> inhalt,
        int seite,
        int groesse,
        long gesamtAnzahl,
        int gesamtSeiten
) {

    public static AntragSeiteResponse von(Page<AntragResponse> page) {
        return new AntragSeiteResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
