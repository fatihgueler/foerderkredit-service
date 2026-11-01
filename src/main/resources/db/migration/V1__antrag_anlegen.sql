-- V1: Tabelle für Förderkredit-Anträge.
-- Enums (rechtsform, branche, bundesland, status) werden als Text gespeichert,
-- passend zu @Enumerated(EnumType.STRING) in der Entity. So bleibt die Datenbank
-- lesbar und eine Umsortierung der Enum-Werte im Java-Code zerstört keine Daten.
-- Geldbeträge sind NUMERIC (exakte Dezimalzahl), niemals FLOAT/DOUBLE.

CREATE TABLE antrag (
    id                  UUID           PRIMARY KEY,
    antragsteller_name  VARCHAR(200)   NOT NULL,
    rechtsform          VARCHAR(30)    NOT NULL,
    branche             VARCHAR(30)    NOT NULL,
    bundesland          VARCHAR(30)    NOT NULL,
    mitarbeiter_anzahl  INTEGER        NOT NULL,
    jahresumsatz        NUMERIC(15, 2) NOT NULL,
    kreditbetrag        NUMERIC(15, 2) NOT NULL,
    laufzeit_jahre      INTEGER        NOT NULL,
    tilgungsfreie_jahre INTEGER        NOT NULL,
    sollzins_prozent    NUMERIC(5, 3)  NOT NULL,
    verwendungszweck    VARCHAR(500)   NOT NULL,
    status              VARCHAR(30)    NOT NULL,
    erstellt_am         TIMESTAMP WITH TIME ZONE NOT NULL,
    geaendert_am        TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Die Liste GET /api/antraege?status=... filtert nach Status.
CREATE INDEX idx_antrag_status ON antrag (status);
