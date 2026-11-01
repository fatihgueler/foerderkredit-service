package de.gueler.foerderkredit.tilgung;

/**
 * Die Eingabewerte erlauben keinen sinnvollen Tilgungsplan, z. B. wenn die
 * tilgungsfreie Zeit genauso lang ist wie die Laufzeit. Ergibt HTTP 422.
 */
public class TilgungsplanNichtBerechenbarException extends RuntimeException {

    public TilgungsplanNichtBerechenbarException(String meldung) {
        super(meldung);
    }
}
