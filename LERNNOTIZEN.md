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

---

## M2 – Regel-Engine und Prüf-Endpoint

### Was gebaut wurde
- **Interface `Foerderregel`** mit genau einer Methode `RegelErgebnis pruefe(Antrag antrag)`.
- **Fünf FIKTIVE Regeln**, jede eine eigene `@Component`: `KreditbetragRegel`, `KmuRegel`,
  `StandortRegel`, `BranchenRegel`, `LaufzeitRegel`.
- **`FoerderpruefungService`** bekommt von Spring alle Regeln als `List<Foerderregel>`, wendet
  alle an und bildet das Gesamtergebnis `foerderfaehig` (nur `true`, wenn ALLE Regeln erfüllt sind).
- **Endpoint** `POST /api/antraege/{id}/pruefung` liefert alle Einzelergebnisse plus Gesamtergebnis.
- **Tests**: je Regel erfüllt/nicht erfüllt inkl. Grenzwerte (z. B. 9.999,99 € / 10.000,00 €,
  249 / 250 Mitarbeitende), Engine-Test mit Lambda-Regeln, Registrierungstest (findet Spring alle 5?),
  Controller-Test.

### Warum so entschieden
- **Strategy-Pattern**: Jede Regel ist eine austauschbare „Strategie“ hinter demselben Interface.
  Die Engine kennt keine konkrete Regel. Neue Regel = neue Klasse mit `@Component`, fertig.
  Es gibt keinen großen `if/else`-Block, der mit jeder Regel wächst (Open-Closed-Prinzip:
  offen für Erweiterung, geschlossen für Änderung).
- **Alle Regeln prüfen statt beim ersten Fehler abbrechen**: Der Sachbearbeiter sieht sofort alle
  Mängel und kann in einer einzigen Rückfrage alles klären.
- **`compareTo` statt `equals` bei `BigDecimal`**: `new BigDecimal("10000.0").equals(new BigDecimal("10000.00"))`
  ist `false`, weil `equals` auch die Anzahl der Nachkommastellen vergleicht. `compareTo` vergleicht
  nur den Wert. In Tests nutze ich deshalb `isEqualByComparingTo`.
- **Begründung als Text**: Banken müssen Entscheidungen nachvollziehbar begründen. Jede Regel sagt
  deshalb nicht nur ja/nein, sondern auch warum.
- **Sortierung nach Regelname**: Die Reihenfolge, in der Spring Beans liefert, ist nicht garantiert.
  Durch Sortieren sieht die API-Antwort immer gleich aus.
- **`POST` für die Prüfung**: Es ist eine fachliche Aktion („Prüfung durchführen“), nicht nur das
  Lesen einer Ressource. Die Prüfung ändert in diesem Projekt (noch) nichts in der Datenbank.

### Interviewfragen
1. **„Wie würden Sie eine sechste Regel hinzufügen?“**
   Eine neue Klasse anlegen, die `Foerderregel` implementiert und mit `@Component` markiert ist,
   plus Unit-Test. Sonst ändert sich nichts; Spring nimmt sie automatisch in die Liste auf.
   Den `RegelRegistrierungTest` würde ich um den neuen Klassennamen ergänzen.
2. **„Wie testen Sie die Engine, ohne von den echten Regeln abhängig zu sein?“**
   `Foerderregel` hat nur eine Methode, ist also ein funktionales Interface. Im Test übergebe ich
   Lambdas wie `a -> RegelErgebnis.nichtErfuellt("A", "...")` und prüfe nur die Engine-Logik.
3. **„Warum sind die Regeln nicht in der Datenbank konfiguriert?“**
   Für dieses Projekt reicht Code: Regeln sind getestet, versioniert und im Code-Review sichtbar.
   Würden Fachbereiche Grenzwerte oft selbst ändern wollen, könnte man die Werte (z. B. 2 Mio. €)
   in eine Konfiguration auslagern. Das wäre aber eine Anforderung, die es hier nicht gibt.

---

## M3 – Tilgungsplan-Rechner und Endpoint

### Was gebaut wurde
- **`TilgungsplanRechner`**: reine Java-Klasse ohne Spring. Berechnet ein Annuitätendarlehen mit
  monatlichen Raten, tilgungsfreier Anlaufzeit und Rundungsausgleich in der letzten Rate.
- **Records `Rate` und `Tilgungsplan`** (Monat, Datum, Zins-, Tilgungsanteil, Rate, Restschuld; Summen).
- **`TilgungsplanService`** holt den Antrag und legt das Datum der ersten Rate fest
  (1. des Monats nach Antragstellung). **Endpoint** `GET /api/antraege/{id}/tilgungsplan`.
- **422 Unprocessable Entity**, wenn mit den Antragsdaten kein Plan möglich ist
  (z. B. tilgungsfreie Jahre ≥ Laufzeit).
- **Tests**: von Hand nachgerechnetes Beispiel (100.000 €, 4 %, 10 Jahre, 1 Jahr tilgungsfrei),
  Grenzfälle (0 tilgungsfreie Jahre, 0,001 % Zins, 0 % Zins, Kleinstbetrag), parametrisierte
  Invarianten-Tests, Controller- und Service-Test.

### So funktioniert die Rechnung (einfach erklärt)
1. Monatszins `i` = Jahreszins / 100 / 12.
2. In der Anlaufzeit zahlt man jeden Monat nur `Restschuld * i` an Zinsen; die Schuld bleibt gleich.
3. Danach ist die Rate jeden Monat gleich hoch (Annuität): `A = K * i * q^n / (q^n - 1)` mit
   `q = 1 + i` und `n` = Anzahl Monate mit Tilgung. Bei 0 % Zins einfach `K / n`.
4. Jeden Monat: Zins = Restschuld * i (gerundet), Tilgung = A − Zins, neue Restschuld = alt − Tilgung.
   Weil die Restschuld sinkt, sinken die Zinsen, und der Tilgungsanteil wächst.
5. Weil auf Cent gerundet wird, bleibt am Ende ein kleiner Rest übrig. Die letzte Rate tilgt deshalb
   genau die Restschuld, und die Restschuld ist exakt 0,00.

### Warum so entschieden
- **`BigDecimal` statt `double`**: `0.1 + 0.2` ergibt mit `double` `0.30000000000000004`, weil
  `double` Zahlen binär speichert. Bei Geld darf kein Cent verloren gehen. `BigDecimal` rechnet
  dezimal und exakt.
- **`MathContext.DECIMAL128`** (34 Stellen) für Zwischenergebnisse: Der Monatszins 0,04/12 ist
  periodisch (0,00333…) und lässt sich nicht exakt darstellen. Ohne `MathContext` würde
  `divide` eine `ArithmeticException` werfen. 34 Stellen sind viel genauer als nötig.
- **`RoundingMode.HALF_UP`** (kaufmännisch: ab 5 aufrunden), und zwar nur dort, wo ein Betrag
  im Plan erscheint. Zwischenschritte werden nicht gerundet, damit sich keine Fehler aufsummieren.
- **Rechner ohne Spring**: Keine Datenbank, keine Beans. Der Test ist ein einfaches `new`, läuft
  in Millisekunden und testet nur die Mathematik.
- **Erwartete Werte unabhängig nachgerechnet**: Wenn ich die Erwartung aus meinem eigenen Code
  übernehme, testet der Test nichts. Die ersten Monate sind mit dem Taschenrechner nachvollziehbar,
  Gesamtzinsen und letzte Rate habe ich zusätzlich mit Python `decimal` nachgerechnet.
- **Datum = 1. des Monats nach Antragstellung**: So ist der Plan für einen Antrag immer gleich, egal
  wann man ihn abruft. Das ist eine vereinfachende Annahme, die im Code dokumentiert ist.
- **`.min(restschuld)`** bei der Tilgung: Bei winzigen Beträgen (z. B. 2,00 € auf 240 Monate) wird
  die Annuität aufgerundet (0,0083 → 0,01). Ohne diese Grenze würde mehr getilgt als offen ist und die
  Restschuld negativ. Ein Test (`wirdBeiKleinstbetragNieNegativ`) sichert das ab.

### Interviewfragen
1. **„Warum kommt in der letzten Rate ein anderer Betrag heraus als in den anderen?“**
   Weil jede Rate auf Cent gerundet wird, entsteht über 108 Monate eine kleine Differenz. Die letzte
   Rate tilgt genau die Restschuld, damit die Schuld exakt 0,00 ist. Das ist bei Banken üblich
   („Schlussrate“). Im Beispiel: 1.103,68 € statt 1.104,10 €.
2. **„Was ist der Unterschied zwischen `setScale(2, HALF_UP)` und `round(new MathContext(2))`?“**
   `setScale(2)` legt die Anzahl der Nachkommastellen fest (Cent). `MathContext(2)` legt die Anzahl
   der *signifikanten* Stellen fest: 1234,56 würde zu 1200. Für Geldbeträge braucht man `setScale`.
3. **„Wie haben Sie sichergestellt, dass Ihre Berechnung stimmt?“**
   Erstens ein Beispiel von Hand nachgerechnet (Rechenweg im Testkommentar), zweitens die Werte
   unabhängig mit einem anderen Werkzeug geprüft, drittens Invarianten getestet, die immer gelten
   müssen: Summe Tilgung = Kreditbetrag, Restschuld am Ende 0,00, nie negativ, Summe der Raten =
   Zinsen + Tilgung.

---

## M4 – Workflow, Statuswechsel, Audit-Log und Historie

### Was gebaut wurde
- **Zustandsautomat in `AntragStatus`**: `erlaubteFolgestatus()` (ein `switch` über alle Status),
  `darfWechselnZu(...)`, `istFinal()`.
- **Flyway `V2__audit_eintrag_anlegen.sql`**: Tabelle `audit_eintrag` mit Fremdschlüssel auf `antrag`.
- **Paket `audit`**: `AuditEintrag` (unveränderliche Entity), `AuditRepository` (nur `save` + Lesen),
  `AuditService`, `HistorieController` (`GET /api/antraege/{id}/historie`).
- **Paket `workflow`**: `StatuswechselService`, `StatuswechselController`
  (`POST /api/antraege/{id}/statuswechsel`), `UngueltigerStatuswechselException` (409),
  `NichtFoerderfaehigException` (422).
- **Tests**: `ZustandsautomatTest` (alle 8 erlaubten Übergänge, Prüfung aller 36 Kombinationen,
  7 benannte verbotene Übergänge, finale Status), Service-Tests mit Mockito, Controller-Tests für
  400/404/409/422, Audit-Tests.

### Warum so entschieden
- **Übergänge zentral im Enum**: Es gibt genau eine Stelle, an der steht, was erlaubt ist. Der
  `switch` hat keinen `default`. Fügt jemand einen neuen Status hinzu, kompiliert der Code nicht, bis
  dessen Übergänge festgelegt sind. So kann man keinen Status „vergessen“.
- **Enum im Paket `antrag`, Logik im Paket `workflow`**: Die Entity `Antrag` braucht den Status als
  Feld. Läge das Enum in `workflow`, würden sich `antrag` und `workflow` gegenseitig kennen
  (Paketzyklus). So zeigen alle Abhängigkeiten in eine Richtung: `workflow → regeln/audit → antrag`.
- **409 Conflict** für verbotene Übergänge: Die Anfrage ist formal richtig, passt aber nicht zum
  *aktuellen Zustand* der Ressource. Genau dafür ist 409 gedacht.
- **422 Unprocessable Entity** für „nicht förderfähig“: Anfrage verstanden und formal richtig, aber
  fachlich nicht ausführbar. Die Antwort nennt die nicht erfüllten Regeln.
- **Eine Transaktion für Statuswechsel und Audit**: `@Transactional` auf `wechsleStatus`.
  `AuditService.protokolliere` hat `Propagation.MANDATORY`: Ohne laufende Transaktion wirft Spring
  sofort einen Fehler. Damit kann niemand den Audit-Eintrag versehentlich in einer eigenen
  Transaktion schreiben. Fliegt irgendwo eine Exception, wird beides zurückgerollt
  (manuell gegen PostgreSQL geprüft: nach einem 422 gibt es keinen zusätzlichen Audit-Eintrag).
- **Kein `save()` beim Statuswechsel**: Der geladene Antrag ist innerhalb der Transaktion von JPA
  verwaltet („managed“). Hibernate merkt die Änderung und schreibt sie beim Commit (Dirty Checking).
- **Revisionssicheres Audit-Log** über drei Ebenen: `@Immutable` (Hibernate schreibt keine Updates),
  keine Setter, und `AuditRepository extends Repository` statt `JpaRepository`, damit es gar keine
  `delete`-Methode gibt.
- **`antragId` als einfache UUID-Spalte** im Audit statt `@ManyToOne`: Der Audit-Eintrag ist ein
  einfacher Datensatz, er soll keine Objekte nachladen. Die Datenbank sichert den Bezug trotzdem
  über den Fremdschlüssel.
- **Die Prüfung beim Bewilligen läuft erneut**: Wir verlassen uns nicht auf ein früheres Ergebnis von
  `/pruefung`. Entscheidend ist der Stand im Moment der Bewilligung.

### Interviewfragen
1. **„Was passiert, wenn das Schreiben des Audit-Eintrags fehlschlägt?“**
   Die Exception verlässt die `@Transactional`-Methode, Spring rollt die ganze Transaktion zurück.
   Der Status bleibt also unverändert. Es kann keinen Statuswechsel ohne Audit-Eintrag geben.
2. **„Warum `Propagation.MANDATORY` und nicht das Standard-`REQUIRED`?“**
   `REQUIRED` würde ohne bestehende Transaktion still eine *neue* starten. Dann könnte jemand
   `protokolliere` außerhalb eines Statuswechsels aufrufen, und Audit und Status wären nicht mehr
   gekoppelt. `MANDATORY` macht diese Annahme ausdrücklich und erzwingt sie.
3. **„Zwei Sachbearbeiter ändern gleichzeitig denselben Antrag. Was passiert?“**
   Ehrliche Antwort: Das ist in diesem Projekt noch nicht abgesichert. Beide könnten denselben alten
   Status lesen. Die Lösung wäre Optimistic Locking mit einer `@Version`-Spalte: Der zweite Commit
   schlägt dann mit einer `OptimisticLockException` fehl und man antwortet mit 409.
