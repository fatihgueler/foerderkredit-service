package de.gueler.foerderkredit.antrag;

import static de.gueler.foerderkredit.antrag.AntragStatus.ABGELEHNT;
import static de.gueler.foerderkredit.antrag.AntragStatus.BEWILLIGT;
import static de.gueler.foerderkredit.antrag.AntragStatus.EINGEREICHT;
import static de.gueler.foerderkredit.antrag.AntragStatus.IN_PRUEFUNG;
import static de.gueler.foerderkredit.antrag.AntragStatus.RUECKFRAGE;
import static de.gueler.foerderkredit.antrag.AntragStatus.ZURUECKGEZOGEN;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** Testet den Zustandsautomaten in {@link AntragStatus#erlaubteFolgestatus()}. */
class ZustandsautomatTest {

    // ---------- Alle erlaubten Übergänge ----------

    @ParameterizedTest(name = "{0} -> {1} ist erlaubt")
    @CsvSource({
            "EINGEREICHT, IN_PRUEFUNG",
            "EINGEREICHT, ZURUECKGEZOGEN",
            "IN_PRUEFUNG, RUECKFRAGE",
            "IN_PRUEFUNG, BEWILLIGT",
            "IN_PRUEFUNG, ABGELEHNT",
            "IN_PRUEFUNG, ZURUECKGEZOGEN",
            "RUECKFRAGE,  IN_PRUEFUNG",
            "RUECKFRAGE,  ZURUECKGEZOGEN"
    })
    void erlaubtDieseUebergaenge(AntragStatus von, AntragStatus nach) {
        assertThat(von.darfWechselnZu(nach)).isTrue();
    }

    @Test
    void erlaubtGenauDieAchtSpezifiziertenUebergaengeUndKeineWeiteren() {
        // Geht ALLE 6 x 6 = 36 Kombinationen durch und sammelt die erlaubten.
        Set<String> erlaubt = new HashSet<>();
        for (AntragStatus von : AntragStatus.values()) {
            for (AntragStatus nach : AntragStatus.values()) {
                if (von.darfWechselnZu(nach)) {
                    erlaubt.add(von + "->" + nach);
                }
            }
        }

        assertThat(erlaubt).containsExactlyInAnyOrder(
                "EINGEREICHT->IN_PRUEFUNG", "EINGEREICHT->ZURUECKGEZOGEN",
                "IN_PRUEFUNG->RUECKFRAGE", "IN_PRUEFUNG->BEWILLIGT",
                "IN_PRUEFUNG->ABGELEHNT", "IN_PRUEFUNG->ZURUECKGEZOGEN",
                "RUECKFRAGE->IN_PRUEFUNG", "RUECKFRAGE->ZURUECKGEZOGEN");
    }

    // ---------- Verbotene Übergänge ----------

    @Test
    void lehntUebergangVonBewilligtNachInPruefungAb() {
        assertThat(BEWILLIGT.darfWechselnZu(IN_PRUEFUNG)).isFalse();
    }

    @Test
    void lehntUebergangVonEingereichtDirektNachBewilligtAb() {
        assertThat(EINGEREICHT.darfWechselnZu(BEWILLIGT)).isFalse();
    }

    @Test
    void lehntUebergangVonEingereichtDirektNachAbgelehntAb() {
        assertThat(EINGEREICHT.darfWechselnZu(ABGELEHNT)).isFalse();
    }

    @Test
    void lehntUebergangVonRueckfrageDirektNachBewilligtAb() {
        assertThat(RUECKFRAGE.darfWechselnZu(BEWILLIGT)).isFalse();
    }

    @Test
    void lehntUebergangVonAbgelehntNachBewilligtAb() {
        assertThat(ABGELEHNT.darfWechselnZu(BEWILLIGT)).isFalse();
    }

    @Test
    void lehntUebergangVonZurueckgezogenNachEingereichtAb() {
        assertThat(ZURUECKGEZOGEN.darfWechselnZu(EINGEREICHT)).isFalse();
    }

    @Test
    void lehntRueckkehrVonInPruefungNachEingereichtAb() {
        assertThat(IN_PRUEFUNG.darfWechselnZu(EINGEREICHT)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(AntragStatus.class)
    void lehntWechselInDenselbenStatusAb(AntragStatus status) {
        assertThat(status.darfWechselnZu(status)).isFalse();
    }

    // ---------- Finale Status ----------

    @ParameterizedTest
    @EnumSource(value = AntragStatus.class, names = {"BEWILLIGT", "ABGELEHNT", "ZURUECKGEZOGEN"})
    void finaleStatusHabenKeineFolgestatus(AntragStatus status) {
        assertThat(status.istFinal()).isTrue();
        assertThat(status.erlaubteFolgestatus()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = AntragStatus.class, names = {"EINGEREICHT", "IN_PRUEFUNG", "RUECKFRAGE"})
    void ausJedemNichtFinalenStatusKannZurueckgezogenWerden(AntragStatus status) {
        assertThat(status.istFinal()).isFalse();
        assertThat(status.darfWechselnZu(ZURUECKGEZOGEN)).isTrue();
    }
}
