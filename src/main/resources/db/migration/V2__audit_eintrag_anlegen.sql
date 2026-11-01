-- V2: Audit-Log für Statuswechsel.
-- Die Anwendung schreibt nur INSERTs in diese Tabelle, niemals UPDATE oder DELETE.
-- kommentar ist optional; alle anderen Felder sind Pflicht.

CREATE TABLE audit_eintrag (
    id          UUID          PRIMARY KEY,
    antrag_id   UUID          NOT NULL REFERENCES antrag (id),
    von_status  VARCHAR(30)   NOT NULL,
    nach_status VARCHAR(30)   NOT NULL,
    kommentar   VARCHAR(500),
    bearbeiter  VARCHAR(100)  NOT NULL,
    zeitpunkt   TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Die Historie eines Antrags wird nach antrag_id gesucht und nach zeitpunkt sortiert.
CREATE INDEX idx_audit_eintrag_antrag_zeitpunkt ON audit_eintrag (antrag_id, zeitpunkt);
