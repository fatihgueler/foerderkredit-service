package de.gueler.foerderkredit;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Startpunkt der Anwendung. {@code @SpringBootApplication} sucht alle Komponenten
 * in diesem Paket und seinen Unterpaketen.
 */
@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Förderkredit-Service",
        version = "1.0",
        description = "Anträge auf Förderkredite anlegen, gegen Förderregeln prüfen, Tilgungspläne berechnen "
                + "und den Bearbeitungsstatus revisionssicher protokollieren. "
                + "Alle Förderregeln sind FIKTIV (Portfolio-Projekt). Fehler kommen als RFC-9457-ProblemDetail."))
public class FoerderkreditApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoerderkreditApplication.class, args);
    }
}
