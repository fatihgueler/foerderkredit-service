package de.gueler.foerderkredit.antrag;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Anlegen und Lesen von Anträgen. */
@Service
public class AntragService {

    private final AntragRepository antragRepository;

    // Konstruktor-Injection: Spring übergibt das Repository. Kein @Autowired nötig,
    // wenn es nur einen Konstruktor gibt. Das Feld kann dadurch final sein.
    public AntragService(AntragRepository antragRepository) {
        this.antragRepository = antragRepository;
    }

    @Transactional
    public AntragResponse anlegen(AntragAnlegenRequest request) {
        Antrag antrag = new Antrag(
                request.antragstellerName(),
                request.rechtsform(),
                request.branche(),
                request.bundesland(),
                request.mitarbeiterAnzahl(),
                request.jahresumsatz(),
                request.kreditbetrag(),
                request.laufzeitJahre(),
                request.tilgungsfreieJahre(),
                request.sollzinsProzent(),
                request.verwendungszweck());
        Antrag gespeichert = antragRepository.save(antrag);
        return AntragResponse.von(gespeichert);
    }

    @Transactional(readOnly = true)
    public AntragResponse lade(UUID id) {
        return AntragResponse.von(findeAntrag(id));
    }

    /** Liefert alle Anträge oder, wenn {@code status} gesetzt ist, nur die mit diesem Status. */
    @Transactional(readOnly = true)
    public AntragSeiteResponse suche(AntragStatus status, Pageable pageable) {
        Page<Antrag> seite = (status == null)
                ? antragRepository.findAll(pageable)
                : antragRepository.findByStatus(status, pageable);
        return AntragSeiteResponse.von(seite.map(AntragResponse::von));
    }

    /**
     * Lädt die Entity für andere Fachpakete (Regeln, Tilgung, Workflow, Audit).
     * Wirft {@link AntragNichtGefundenException}, wenn es die ID nicht gibt.
     */
    @Transactional(readOnly = true)
    public Antrag findeAntrag(UUID id) {
        return antragRepository.findById(id)
                .orElseThrow(() -> new AntragNichtGefundenException(id));
    }
}
