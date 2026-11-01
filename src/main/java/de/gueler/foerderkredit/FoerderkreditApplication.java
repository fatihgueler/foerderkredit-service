package de.gueler.foerderkredit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Startpunkt der Anwendung. {@code @SpringBootApplication} sucht alle Komponenten
 * in diesem Paket und seinen Unterpaketen.
 */
@SpringBootApplication
public class FoerderkreditApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoerderkreditApplication.class, args);
    }
}
