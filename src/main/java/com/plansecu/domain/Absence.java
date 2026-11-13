package com.plansecu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "absence")
@Getter
@Setter
@NoArgsConstructor
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeAbsence type = TypeAbsence.CONGE;

    @Column(length = 500)
    private String motif;

    public long getNombreJours() {
        return ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;
    }

    public boolean couvre(LocalDate date) {
        return !date.isBefore(dateDebut) && !date.isAfter(dateFin);
    }
}
