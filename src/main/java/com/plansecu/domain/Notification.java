package com.plansecu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Journal des SMS / emails envoyés aux agents. */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Column(nullable = false, length = 150)
    private String destinataire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CanalNotification canal;

    @Column(length = 200)
    private String sujet;

    @Column(nullable = false, length = 4000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutNotification statut;

    @Column(length = 500)
    private String erreur;

    @Column(nullable = false)
    private LocalDateTime envoyeLe;
}
