package de.gueler.foerderkredit.antrag;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Datenbankzugriff für Anträge. Spring Data erzeugt die Implementierung zur Laufzeit;
 * die Abfrage für {@link #findByStatus} wird aus dem Methodennamen abgeleitet.
 */
public interface AntragRepository extends JpaRepository<Antrag, UUID> {

    Page<Antrag> findByStatus(AntragStatus status, Pageable pageable);
}
