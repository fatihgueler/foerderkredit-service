# Lernnotizen – Förderkredit-Service

Pro Meilenstein: was gebaut wurde, warum so entschieden wurde, und typische Interviewfragen
mit kurzer Musterantwort. Ziel: jede Zeile im Code erklären können.

---

## M1 – Projektgerüst, Datenbank, Antrag anlegen und lesen

### Was gebaut wurde
- **Maven-Projekt mit Wrapper** (`mvnw`, `mvnw.cmd`): Der Wrapper lädt die passende Maven-Version
  selbst herunter. Man braucht nur ein JDK 21, keine globale Maven-Installation.
- **`pom.xml`** mit Spring Boot 3.5 als Parent. Der Parent legt die Versionen aller Bibliotheken fest,
  die zusammenpassen. Nur springdoc braucht eine eigene Versionsnummer.
- **`docker-compose.yml`** mit PostgreSQL 16 für die lokale Entwicklung.
- **Flyway-Migration `V1__antrag_anlegen.sql`**: legt die Tabelle `antrag` an.
- **Paket `antrag`**: Enums (`Rechtsform`, `Branche`, `Bundesland`, `AntragStatus`), Entity `Antrag`,
  `AntragRepository`, DTOs als `record`s, `AntragService`, `AntragController`.
- **Paket `common`**: `GlobalExceptionHandler`, der alle Fehler als RFC-9457-`ProblemDetail`
  mit deutscher Meldung beantwortet.
- **Tests**: `AntragControllerTest` (MockMvc), `AntragServiceTest` (Mockito), `AntragRepositoryIT`
  (Testcontainers, wird ohne Docker übersprungen), `AntragTestdaten` (Test-Data-Builder).

### Warum so entschieden
- **Flyway statt `ddl-auto=update`**: Das Schema steht versioniert in SQL-Dateien im Git.
  Jede Änderung ist nachvollziehbar und läuft auf jedem Rechner und in Produktion gleich.
  Hibernate prüft mit `validate` nur, ob Entity und Tabelle zusammenpassen, und startet sonst nicht.
  In einer Bank will man nie, dass ein Framework „heimlich“ Tabellen ändert.
- **Entity und DTO getrennt**: Die Entity `Antrag` ist das Datenbankmodell. Nach außen gehen nur
  `AntragResponse`/`AntragAnlegenRequest`. So kann der Client z. B. nicht selbst `status` oder `id`
  setzen, und Datenbankänderungen brechen die API nicht.
- **Keine Setter in der Entity**: Der Status ändert sich nur über `wechsleStatus(...)`. So kann
  niemand aus Versehen den Kreditbetrag eines eingereichten Antrags überschreiben.
- **Validierung ≠ Förderregeln**: Bean Validation prüft nur das Format (Pflichtfeld, Länge,
  Zahlenformat) → 400. Ob ein Antrag förderfähig ist, entscheiden die Regeln in M2. Ein Antrag
  über 5.000 € wird also gespeichert, ist aber später nicht förderfähig.
- **Wrapper-Typen im Request** (`Integer` statt `int`): Fehlt ein Feld im JSON, wäre ein `int`
  einfach `0` und man würde den Fehler nicht bemerken. `Integer` ist dann `null`, und `@NotNull` greift.
- **`BigDecimal` + `NUMERIC(15,2)`** für Geld: exakt, keine Rundungsfehler wie bei `double`.
  `setScale(2)` im Konstruktor sorgt dafür, dass ein Betrag immer genau 2 Nachkommastellen hat.
- **Enums als Text (`EnumType.STRING`)**: In der Datenbank steht `NIEDERSACHSEN`, nicht `8`.
  Würde jemand die Reihenfolge im Enum ändern, wären Zahlenwerte (ORDINAL) plötzlich falsch.
- **`open-in-view: false`**: Datenbankzugriffe passieren nur in den `@Transactional`-Service-Methoden,
  nicht versteckt später beim JSON-Schreiben.
- **Eigenes `AntragSeiteResponse`** statt Springs `Page` zurückzugeben: Das JSON-Format der Liste
  bestimmen wir selbst und es ändert sich nicht bei einem Spring-Update.
- **Konstruktor-Injection**: Abhängigkeiten sind `final`, die Klasse ist ohne Spring im Unit-Test
  erzeugbar (siehe `AntragServiceTest` mit `@InjectMocks`).

### Interviewfragen
1. **„Warum geben Sie die Entity nicht direkt als JSON zurück?“**
   Weil Datenbankmodell und API sich unabhängig ändern sollen. Außerdem könnte ein Client sonst Felder
   wie `status` oder `id` mitschicken und setzen. Mit DTOs bestimme ich genau, was rein- und rausgeht.
2. **„Was passiert, wenn jemand die Entity ändert, aber keine Flyway-Migration schreibt?“**
   Die Anwendung startet nicht, weil `ddl-auto=validate` beim Start feststellt, dass Spalte oder Typ
   fehlen. Der Fehler fällt also sofort auf (spätestens im Integrationstest in der CI) und nicht erst
   in Produktion.
3. **„Was ist der Unterschied zwischen 400 bei Validierung und einem nicht förderfähigen Antrag?“**
   400 heißt: Die Anfrage ist formal kaputt, z. B. Pflichtfeld fehlt. Ein nicht förderfähiger Antrag
   ist formal korrekt und wird gespeichert; er erfüllt nur fachliche Regeln nicht. Das ist ein
   fachliches Ergebnis, kein technischer Fehler.
