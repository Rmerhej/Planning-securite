package com.plansecu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent")
@Getter
@Setter
@NoArgsConstructor
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, length = 20)
    private String telephone;

    private String email;

    /** Carte professionnelle CNAPS. */
    private String carteProNumero;
    private LocalDate carteProExpiration;

    /** Couleur de l'agent dans le calendrier. */
    @Column(nullable = false, length = 7)
    private String couleur = "#3b82f6";

    private boolean actif = true;
    private boolean notifSms = true;
    private boolean notifEmail = true;
    private boolean recoitHebdo = true;
    private boolean recoitMensuel = false;

    /** Jeton secret de l'URL d'abonnement calendrier (.ics). */
    @Column(nullable = false, length = 40)
    private String calendrierToken;

    @Column(nullable = false)
    private LocalDateTime creeLe;

    @PrePersist
    void prePersist() {
        if (creeLe == null) creeLe = LocalDateTime.now();
        if (calendrierToken == null) regenererToken();
    }

    public void regenererToken() {
        calendrierToken = UUID.randomUUID().toString().replace("-", "");
    }

    /** "DUPONT Jean" */
    public String getNomComplet() {
        return nom.toUpperCase() + " " + prenom;
    }

    /** "J. Dupont" */
    public String getNomCourt() {
        return (prenom.isEmpty() ? "" : prenom.charAt(0) + ". ") + nom;
    }

    /** "06 12 34 56 78" */
    public String getTelephoneFormate() {
        String chiffres = telephone == null ? "" : telephone.replaceAll("\\D", "");
        if (chiffres.length() == 10) {
            return chiffres.replaceAll("(\\d{2})(?=\\d)", "$1 ");
        }
        return telephone;
    }

    /** "DUPONT Jean · 06 12 34 56 78" */
    public String getNomEtTelephone() {
        return getNomComplet() + " · " + getTelephoneFormate();
    }

    public boolean carteProValideLe(LocalDate date) {
        return carteProExpiration == null || !carteProExpiration.isBefore(date);
    }
}
