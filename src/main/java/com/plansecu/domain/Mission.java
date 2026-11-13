package com.plansecu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Affectation d'un agent sur un site pour une date, avec un ou plusieurs créneaux. */
@Entity
@Table(name = "mission")
@Getter
@Setter
@NoArgsConstructor
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_mission", nullable = false)
    private LocalDate dateMission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutMission statut = StatutMission.PLANIFIEE;

    @Column(length = 500)
    private String commentaire;

    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("heureDebut ASC")
    private List<Creneau> creneaux = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime creeLe;

    @Column(nullable = false)
    private LocalDateTime modifieLe;

    @PrePersist
    void prePersist() {
        creeLe = LocalDateTime.now();
        modifieLe = creeLe;
    }

    @PreUpdate
    void preUpdate() {
        modifieLe = LocalDateTime.now();
    }

    public void ajouterCreneau(LocalTime debut, LocalTime fin) {
        Creneau c = new Creneau(debut, fin);
        c.setMission(this);
        creneaux.add(c);
    }

    public List<Creneau> getCreneauxTries() {
        return creneaux.stream().sorted(Comparator.comparing(Creneau::getHeureDebut)).toList();
    }

    public long getDureeMinutes() {
        return creneaux.stream().mapToLong(Creneau::getDureeMinutes).sum();
    }

    /** "09h00–12h00 / 15h00–18h00" */
    public String getHorairesLibelle() {
        return getCreneauxTries().stream().map(Creneau::getLibelle).collect(Collectors.joining(" / "));
    }

    public LocalTime getPremiereHeure() {
        return creneaux.stream().map(Creneau::getHeureDebut).min(Comparator.naturalOrder()).orElse(LocalTime.MIN);
    }

    public boolean occupeAgent() {
        return statut != null && statut.occupeAgent();
    }
}
