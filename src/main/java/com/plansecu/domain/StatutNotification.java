package com.plansecu.domain;

public enum StatutNotification {
    ENVOYE("Envoyé", "success"),
    SIMULE("Simulé", "info"),
    ECHEC("Échec", "danger");

    private final String libelle;
    private final String couleur;

    StatutNotification(String libelle, String couleur) {
        this.libelle = libelle;
        this.couleur = couleur;
    }

    public String getLibelle() { return libelle; }
    public String getCouleur() { return couleur; }
}
