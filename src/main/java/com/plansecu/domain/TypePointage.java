package com.plansecu.domain;

public enum TypePointage {
    ARRIVEE("Arrivée"),
    DEPART("Départ");

    private final String libelle;

    TypePointage(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }
}
