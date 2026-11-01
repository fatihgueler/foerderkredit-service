package de.gueler.foerderkredit.antrag;

import java.util.Set;

/**
 * Bearbeitungsstatus eines Antrags und zugleich der Zustandsautomat des Workflows.
 *
 * <pre>
 * EINGEREICHT ──► IN_PRUEFUNG ──► BEWILLIGT   (final)
 *                   ▲     │  └──► ABGELEHNT   (final)
 *                   │     ▼
 *                 RUECKFRAGE
 *
 * ZURUECKGEZOGEN (final) ist aus jedem nicht-finalen Status erreichbar.
 * </pre>
 *
 * <p>Die erlaubten Übergänge stehen an genau einer Stelle: in {@link #erlaubteFolgestatus()}.
 * Der {@code switch} hat keinen {@code default}-Zweig. Kommt ein neuer Status hinzu, meldet der
 * Compiler deshalb einen Fehler, bis man auch dessen Übergänge festgelegt hat.
 */
public enum AntragStatus {
    EINGEREICHT,
    IN_PRUEFUNG,
    RUECKFRAGE,
    BEWILLIGT,
    ABGELEHNT,
    ZURUECKGEZOGEN;

    /** Alle Status, in die man aus diesem Status wechseln darf. */
    public Set<AntragStatus> erlaubteFolgestatus() {
        return switch (this) {
            case EINGEREICHT -> Set.of(IN_PRUEFUNG, ZURUECKGEZOGEN);
            case IN_PRUEFUNG -> Set.of(RUECKFRAGE, BEWILLIGT, ABGELEHNT, ZURUECKGEZOGEN);
            case RUECKFRAGE -> Set.of(IN_PRUEFUNG, ZURUECKGEZOGEN);
            case BEWILLIGT, ABGELEHNT, ZURUECKGEZOGEN -> Set.of();
        };
    }

    public boolean darfWechselnZu(AntragStatus neuerStatus) {
        return erlaubteFolgestatus().contains(neuerStatus);
    }

    /** Final heißt: Aus diesem Status gibt es keinen Ausweg mehr. */
    public boolean istFinal() {
        return erlaubteFolgestatus().isEmpty();
    }
}
