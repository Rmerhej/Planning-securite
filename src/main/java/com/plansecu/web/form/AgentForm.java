package com.plansecu.web.form;

import com.plansecu.domain.Agent;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class AgentForm {

    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    private String prenom;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^[+0-9 .()-]{10,20}$", message = "Numéro de téléphone invalide")
    private String telephone;

    @Email(message = "Email invalide")
    @Size(max = 150)
    private String email;

    @Size(max = 50)
    private String carteProNumero;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate carteProExpiration;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Couleur invalide")
    private String couleur = "#3b82f6";

    private boolean actif = true;
    private boolean notifSms = true;
    private boolean notifEmail = true;
    private boolean recoitHebdo = true;
    private boolean recoitMensuel = false;

    /** Compte de connexion de l'agent (facultatif). */
    @Size(max = 100)
    private String identifiant;

    @Size(max = 100)
    private String motDePasse;

    public static AgentForm depuis(Agent a, String identifiant) {
        AgentForm f = new AgentForm();
        f.setId(a.getId());
        f.setNom(a.getNom());
        f.setPrenom(a.getPrenom());
        f.setTelephone(a.getTelephone());
        f.setEmail(a.getEmail());
        f.setCarteProNumero(a.getCarteProNumero());
        f.setCarteProExpiration(a.getCarteProExpiration());
        f.setCouleur(a.getCouleur());
        f.setActif(a.isActif());
        f.setNotifSms(a.isNotifSms());
        f.setNotifEmail(a.isNotifEmail());
        f.setRecoitHebdo(a.isRecoitHebdo());
        f.setRecoitMensuel(a.isRecoitMensuel());
        f.setIdentifiant(identifiant);
        return f;
    }

    public void appliquer(Agent a) {
        a.setNom(nom.trim());
        a.setPrenom(prenom.trim());
        a.setTelephone(telephone.trim());
        a.setEmail(email == null || email.isBlank() ? null : email.trim());
        a.setCarteProNumero(carteProNumero == null || carteProNumero.isBlank() ? null : carteProNumero.trim());
        a.setCarteProExpiration(carteProExpiration);
        a.setCouleur(couleur);
        a.setActif(actif);
        a.setNotifSms(notifSms);
        a.setNotifEmail(notifEmail);
        a.setRecoitHebdo(recoitHebdo);
        a.setRecoitMensuel(recoitMensuel);
    }
}
