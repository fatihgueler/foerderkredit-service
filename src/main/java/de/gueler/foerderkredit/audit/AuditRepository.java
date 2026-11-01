package de.gueler.foerderkredit.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Bewusst NICHT {@code JpaRepository}: Das würde automatisch auch {@code delete...}- und
 * {@code saveAll}-Methoden anbieten. Das schlanke Marker-Interface {@link Repository}
 * hat gar keine Methoden; Spring Data erzeugt nur die, die hier stehen.
 * So gibt es im Code schlicht keine Möglichkeit, einen Audit-Eintrag zu löschen.
 */
public interface AuditRepository extends Repository<AuditEintrag, UUID> {

    AuditEintrag save(AuditEintrag eintrag);

    List<AuditEintrag> findByAntragIdOrderByZeitpunktAsc(UUID antragId);
}
