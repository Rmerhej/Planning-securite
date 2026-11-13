package com.plansecu.service;

import com.plansecu.domain.Mission;
import com.plansecu.domain.Pointage;

import java.util.List;

/**
 * Comparaison prévu / pointé pour une mission.
 *
 * @param minutesPointees durée entre chaque arrivée et le départ qui la suit
 * @param retardMinutes   cumul des retards au-delà de la tolérance
 * @param anomalies       messages pour l'admin (retard, pas d'arrivée, départ non pointé…)
 */
public record SuiviPointage(Mission mission,
                            List<Pointage> pointages,
                            long minutesPointees,
                            long retardMinutes,
                            Etat etat,
                            List<String> anomalies) {

    public enum Etat {
        A_VENIR("À venir", "light"),
        EN_COURS("En poste", "primary"),
        TERMINEE("Pointée", "success"),
        ANOMALIE("À vérifier", "warning"),
        NON_POINTEE("Non pointée", "danger"),
        SANS_OBJET("—", "light");

        private final String libelle;
        private final String couleur;

        Etat(String libelle, String couleur) {
            this.libelle = libelle;
            this.couleur = couleur;
        }

        public String getLibelle() { return libelle; }
        public String getCouleur() { return couleur; }
    }

    public String getHeuresPointees() {
        return CalculHeures.format(minutesPointees);
    }

    /** Écart pointé − prévu, en minutes (négatif = moins d'heures que prévu). */
    public long getEcartMinutes() {
        return minutesPointees - mission.getDureeMinutes();
    }
}
