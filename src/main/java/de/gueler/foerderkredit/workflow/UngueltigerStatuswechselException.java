package de.gueler.foerderkredit.workflow;

import de.gueler.foerderkredit.antrag.AntragStatus;
import java.util.Set;

/** Der gewünschte Übergang ist im Zustandsautomaten nicht erlaubt. Ergibt HTTP 409 Conflict. */
public class UngueltigerStatuswechselException extends RuntimeException {

    private final AntragStatus vonStatus;
    private final AntragStatus nachStatus;

    public UngueltigerStatuswechselException(AntragStatus vonStatus, AntragStatus nachStatus) {
        super(meldung(vonStatus, nachStatus));
        this.vonStatus = vonStatus;
        this.nachStatus = nachStatus;
    }

    public AntragStatus getVonStatus() {
        return vonStatus;
    }

    public AntragStatus getNachStatus() {
        return nachStatus;
    }

    public Set<AntragStatus> getErlaubteFolgestatus() {
        return vonStatus.erlaubteFolgestatus();
    }

    private static String meldung(AntragStatus von, AntragStatus nach) {
        if (von.istFinal()) {
            return "Der Antrag ist im finalen Status " + von + " und kann nicht mehr geändert werden.";
        }
        return "Ein Wechsel von " + von + " nach " + nach + " ist nicht erlaubt.";
    }
}
