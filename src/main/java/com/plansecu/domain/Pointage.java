package com.plansecu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Arrivée ou départ d'un agent sur une mission, par scan du QR code du lieu (ou saisie admin). */
@Entity
@Table(name = "pointage")
@Getter
@Setter
@NoArgsConstructor
public class Pointage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TypePointage type;

    @Column(nullable = false)
    private LocalDateTime horodatage;

    private boolean manuel;

    @Column(length = 100)
    private String saisiPar;

    @Column(length = 255)
    private String commentaire;

    public Pointage(Mission mission, TypePointage type, LocalDateTime horodatage) {
        this.mission = mission;
        this.type = type;
        this.horodatage = horodatage;
    }
}
