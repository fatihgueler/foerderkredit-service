package de.gueler.foerderkredit.antrag;

import java.util.UUID;

/** Wird geworfen, wenn es zu einer ID keinen Antrag gibt. Ergibt HTTP 404. */
public class AntragNichtGefundenException extends RuntimeException {

    public AntragNichtGefundenException(UUID id) {
        super("Es gibt keinen Antrag mit der ID " + id + ".");
    }
}
