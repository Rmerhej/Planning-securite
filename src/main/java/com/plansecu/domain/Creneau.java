package com.plansecu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Plage horaire d'une mission. Si heureFin <= heureDebut, le créneau se termine le lendemain
 * (ex. 21h00 - 06h00).
 */
@Entity
@Table(name = "creneau")
@Getter
@Setter
@NoArgsConstructor
public class Creneau {

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH'h'mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @Column(nullable = false)
    private LocalTime heureDebut;

    @Column(nullable = false)
    private LocalTime heureFin;

    public Creneau(LocalTime heureDebut, LocalTime heureFin) {
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
    }

    public boolean passeMinuit() {
        return !heureFin.isAfter(heureDebut);
    }

    public LocalDateTime debut(LocalDate date) {
        return date.atTime(heureDebut);
    }

    public LocalDateTime fin(LocalDate date) {
        return passeMinuit() ? date.plusDays(1).atTime(heureFin) : date.atTime(heureFin);
    }

    public long getDureeMinutes() {
        LocalDate ref = LocalDate.of(2000, 1, 1);
        return Duration.between(debut(ref), fin(ref)).toMinutes();
    }

    /** "09h00–12h00" ou "21h00–06h00 (+1)" */
    public String getLibelle() {
        return HEURE.format(heureDebut) + "–" + HEURE.format(heureFin) + (passeMinuit() ? " (+1)" : "");
    }
}
