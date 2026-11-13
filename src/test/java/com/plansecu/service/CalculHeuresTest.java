package com.plansecu.service;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.Mission;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class CalculHeuresTest {

    private final CalculHeures calcul = new CalculHeures(new AppProperties());

    private static Mission mission(LocalDate date, int... heures) {
        Mission m = new Mission();
        m.setDateMission(date);
        for (int i = 0; i < heures.length; i += 2) {
            m.ajouterCreneau(LocalTime.of(heures[i], 0), LocalTime.of(heures[i + 1], 0));
        }
        return m;
    }

    @Test
    void missionDiscontinue() {
        Mission m = mission(LocalDate.of(2026, 11, 3), 9, 12, 15, 18);
        assertThat(m.getDureeMinutes()).isEqualTo(360);
        assertThat(calcul.minutesNuit(m)).isZero();
        assertThat(m.getHorairesLibelle()).isEqualTo("09h00–12h00 / 15h00–18h00");
    }

    @Test
    void creneauDeNuitPassantMinuit() {
        Mission m = mission(LocalDate.of(2026, 11, 3), 22, 6);
        assertThat(m.getDureeMinutes()).isEqualTo(480);
        assertThat(calcul.minutesNuit(m)).isEqualTo(480);
    }

    @Test
    void nuitDuSamediAuDimancheFerie() {
        // Samedi 31/10/2026 20h -> dimanche 1er novembre (Toussaint) 2h
        Mission m = mission(LocalDate.of(2026, 10, 31), 20, 2);
        assertThat(m.getDureeMinutes()).isEqualTo(360);
        assertThat(calcul.minutesNuit(m)).isEqualTo(300);       // 21h -> 2h
        assertThat(calcul.minutesDimanche(m)).isEqualTo(120);   // 0h -> 2h
        assertThat(calcul.minutesFerie(m)).isEqualTo(120);
    }

    @Test
    void joursFeries() {
        assertThat(JoursFeries.paques(2026)).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(JoursFeries.paques(2027)).isEqualTo(LocalDate.of(2027, 3, 28));
        assertThat(JoursFeries.estFerie(LocalDate.of(2026, 4, 6))).isTrue();   // lundi de Pâques
        assertThat(JoursFeries.estFerie(LocalDate.of(2026, 5, 14))).isTrue();  // Ascension
        assertThat(JoursFeries.estFerie(LocalDate.of(2026, 5, 25))).isTrue();  // lundi de Pentecôte
        assertThat(JoursFeries.estFerie(LocalDate.of(2026, 11, 2))).isFalse();
    }
}
