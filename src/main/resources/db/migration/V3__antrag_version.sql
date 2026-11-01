-- V3: Versionsspalte für Optimistic Locking.
-- Hibernate erhöht den Wert bei jeder Änderung um 1 und prüft beim UPDATE,
-- ob die Version in der Datenbank noch die ist, die beim Laden gelesen wurde:
--   UPDATE antrag SET ..., version = 1 WHERE id = ? AND version = 0
-- Trifft das UPDATE keine Zeile, hat jemand anderes den Antrag inzwischen geändert.
-- DEFAULT 0 sorgt dafür, dass bereits vorhandene Anträge einen gültigen Startwert bekommen.

ALTER TABLE antrag ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
