package com.plansecu.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Lieu de mission (magasin en général). */
@Entity
@Table(name = "site")
@Getter
@Setter
@NoArgsConstructor
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 150)
    private String nom;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255)
    private String adresse;

    @Size(max = 10)
    private String codePostal;

    @Size(max = 100)
    private String ville;

    @Size(max = 150)
    private String client;

    @Size(max = 150)
    private String contactNom;

    @Size(max = 20)
    private String contactTelephone;

    @Size(max = 2000)
    private String consignes;

    private boolean actif = true;

    /** Secret contenu dans le QR code affiché sur le lieu, pour le pointage. */
    @Column(length = 40, unique = true)
    private String qrToken;

    @PrePersist
    void prePersist() {
        if (qrToken == null) regenererQr();
    }

    public void regenererQr() {
        qrToken = UUID.randomUUID().toString().replace("-", "");
    }

    public String getAdresseComplete() {
        StringBuilder sb = new StringBuilder(adresse == null ? "" : adresse);
        String cpVille = ((codePostal == null ? "" : codePostal) + " " + (ville == null ? "" : ville)).trim();
        if (!cpVille.isEmpty()) sb.append(", ").append(cpVille);
        return sb.toString();
    }

    public String getLienCarte() {
        return "https://www.google.com/maps/search/?api=1&query="
                + URLEncoder.encode(nom + " " + getAdresseComplete(), StandardCharsets.UTF_8);
    }
}
