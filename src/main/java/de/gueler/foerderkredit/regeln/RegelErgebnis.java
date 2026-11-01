package de.gueler.foerderkredit.regeln;

/** Ergebnis einer einzelnen Regel: Name, erfüllt ja/nein und eine verständliche Begründung. */
public record RegelErgebnis(String regelName, boolean erfuellt, String begruendung) {

    public static RegelErgebnis erfuellt(String regelName, String begruendung) {
        return new RegelErgebnis(regelName, true, begruendung);
    }

    public static RegelErgebnis nichtErfuellt(String regelName, String begruendung) {
        return new RegelErgebnis(regelName, false, begruendung);
    }
}
