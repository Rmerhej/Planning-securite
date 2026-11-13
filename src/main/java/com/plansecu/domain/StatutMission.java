package com.plansecu.domain;

public enum StatutMission {
    PLANIFIEE("Planifiée", "primary"),
    EFFECTUEE("Effectuée", "success"),
    ABSENT("Agent absent", "danger"),
    ANNULEE("Annulée", "secondary");

    private final String libelle;
    private final String couleur;

    StatutMission(String libelle, String couleur) {
        this.libelle = libelle;
        this.couleur = couleur;
    }

    public String getLibelle() { return libelle; }

    /** Couleur Bootstrap (badge). */
    public String getCouleur() { return couleur; }

    /** La mission occupe-t-elle réellement l'agent ? */
    public boolean occupeAgent() {
        return this == PLANIFIEE || this == EFFECTUEE;
    }
}
