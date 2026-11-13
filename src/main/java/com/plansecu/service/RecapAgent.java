package com.plansecu.service;

import com.plansecu.domain.Agent;
import com.plansecu.domain.TypeAbsence;

import java.util.Map;
import java.util.stream.Collectors;

/** Récapitulatif mensuel d'un agent. Les durées sont en minutes. */
public record RecapAgent(
        Agent agent,
        int joursTravailles,
        int missionsEffectuees,
        long minutesEffectuees,
        long minutesNuit,
        long minutesDimanche,
        long minutesFerie,
        long minutesPlanifiees,
        int joursAbsence,
        Map<TypeAbsence, Integer> absencesParType,
        Map<String, Long> minutesParSite,
        long minutesPointees,
        int missionsNonPointees) {

    public String getHeuresPointees() { return CalculHeures.format(minutesPointees); }

    /** Écart pointé − effectué ("+0h15", "-1h30"), ou vide si rien n'a été pointé. */
    public String getEcartPointage() {
        if (minutesPointees == 0) return "";
        long e = minutesPointees - minutesEffectuees;
        return (e < 0 ? "-" : "+") + CalculHeures.format(Math.abs(e));
    }

    public String getHeuresEffectuees() { return CalculHeures.format(minutesEffectuees); }
    public String getHeuresNuit() { return CalculHeures.format(minutesNuit); }
    public String getHeuresDimanche() { return CalculHeures.format(minutesDimanche); }
    public String getHeuresFerie() { return CalculHeures.format(minutesFerie); }
    public String getHeuresPlanifiees() { return CalculHeures.format(minutesPlanifiees); }

    /** "Congés : 3 j · Maladie : 1 j" */
    public String getDetailAbsences() {
        return absencesParType.entrySet().stream()
                .map(e -> e.getKey().getLibelle() + " : " + e.getValue() + " j")
                .collect(Collectors.joining(" · "));
    }
}
