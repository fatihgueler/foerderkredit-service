package de.gueler.foerderkredit.regeln;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import de.gueler.foerderkredit.antrag.Antrag;
import de.gueler.foerderkredit.antrag.AntragService;
import de.gueler.foerderkredit.antrag.AntragTestdaten;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testet die Engine mit einfachen Test-Regeln (Lambdas), unabhängig von den echten Regeln.
 * Das geht, weil {@link Foerderregel} nur eine Methode hat.
 */
class FoerderpruefungServiceTest {

    private final Antrag antrag = AntragTestdaten.gueltig().baue();

    @Test
    void istFoerderfaehigWennAlleRegelnErfuelltSind() {
        FoerderpruefungService service = serviceMit(
                a -> RegelErgebnis.erfuellt("A", "ok"),
                a -> RegelErgebnis.erfuellt("B", "ok"));

        PruefungsErgebnis ergebnis = service.pruefe(antrag);

        assertThat(ergebnis.foerderfaehig()).isTrue();
        assertThat(ergebnis.regelergebnisse()).hasSize(2);
    }

    @Test
    void istNichtFoerderfaehigWennEineRegelNichtErfuelltIst() {
        FoerderpruefungService service = serviceMit(
                a -> RegelErgebnis.erfuellt("A", "ok"),
                a -> RegelErgebnis.nichtErfuellt("B", "nicht ok"));

        assertThat(service.pruefe(antrag).foerderfaehig()).isFalse();
    }

    @Test
    void liefertAlleRegelergebnisseAuchNachEinemFehlschlag() {
        // Die erste Regel schlägt fehl; trotzdem müssen alle drei Ergebnisse kommen.
        FoerderpruefungService service = serviceMit(
                a -> RegelErgebnis.nichtErfuellt("A", "nicht ok"),
                a -> RegelErgebnis.nichtErfuellt("B", "nicht ok"),
                a -> RegelErgebnis.erfuellt("C", "ok"));

        PruefungsErgebnis ergebnis = service.pruefe(antrag);

        assertThat(ergebnis.regelergebnisse())
                .extracting(RegelErgebnis::regelName)
                .containsExactly("A", "B", "C");
    }

    @Test
    void sortiertDieErgebnisseNachRegelname() {
        FoerderpruefungService service = serviceMit(
                a -> RegelErgebnis.erfuellt("Zeta", "ok"),
                a -> RegelErgebnis.erfuellt("Alpha", "ok"));

        assertThat(service.pruefe(antrag).regelergebnisse())
                .extracting(RegelErgebnis::regelName)
                .containsExactly("Alpha", "Zeta");
    }

    @Test
    void echterBeispielantragIstMitDenFiktivenRegelnFoerderfaehig() {
        FoerderpruefungService service = serviceMit(new KreditbetragRegel(), new KmuRegel(),
                new StandortRegel(), new BranchenRegel(), new LaufzeitRegel());

        PruefungsErgebnis ergebnis = service.pruefe(antrag);

        assertThat(ergebnis.foerderfaehig()).isTrue();
        assertThat(ergebnis.regelergebnisse()).hasSize(5).allMatch(RegelErgebnis::erfuellt);
    }

    private static FoerderpruefungService serviceMit(Foerderregel... regeln) {
        return new FoerderpruefungService(mock(AntragService.class), List.of(regeln));
    }
}
