package com.plansecu.domain;

public enum TypeAbsence {
    CONGE("Congés"),
    MALADIE("Maladie"),
    INJUSTIFIEE("Absence injustifiée"),
    FORMATION("Formation"),
    AUTRE("Autre");

    private final String libelle;

    TypeAbsence(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }
}
